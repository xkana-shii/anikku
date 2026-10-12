package eu.kanade.tachiyomi.data.track.simkl

import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.data.track.simkl.dto.SimklAnimeResponse
import eu.kanade.tachiyomi.data.track.simkl.dto.SimklOAuth
import eu.kanade.tachiyomi.data.track.simkl.dto.SimklSearchResult
import eu.kanade.tachiyomi.data.track.simkl.dto.SimklSyncResult
import eu.kanade.tachiyomi.data.track.simkl.dto.SimklSyncWatched
import eu.kanade.tachiyomi.data.track.simkl.dto.SimklUser
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.injectLazy
import tachiyomi.domain.track.model.Track as DomainTrack

class SimklApi(private val client: OkHttpClient, interceptor: SimklInterceptor) {

    private val json: Json by injectLazy()

    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    suspend fun addLibAnime(track: Track): Track {
        return withIOContext {
            val type = track.tracking_url
                .substringAfter("/")
                .substringBefore("/")
            val mediaType = if (type == "movies") "movies" else "shows"
            addToList(track, mediaType)

            track
        }
    }

    private suspend fun addToList(track: Track, mediaType: String) {
        val payload = buildJsonObject {
            putJsonArray(mediaType) {
                addJsonObject {
                    putJsonObject("ids") {
                        put("simkl", track.remote_id)
                    }
                    put("to", track.toSimklStatus())
                }
            }
        }.toString().toRequestBody(jsonMime)
        authClient.newCall(
            POST("$API_URL/sync/add-to-list", body = payload),
        ).awaitSuccess()
    }

    private suspend fun updateRating(track: Track, mediaType: String) {
        val payload = buildJsonObject {
            putJsonArray(mediaType) {
                addJsonObject {
                    putJsonObject("ids") {
                        put("simkl", track.remote_id)
                    }
                    put("rating", track.score.toInt())
                }
            }
        }.toString().toRequestBody(jsonMime)

        if (track.score == 0.0) {
            authClient.newCall(
                POST("$API_URL/sync/ratings/remove", body = payload),
            ).awaitSuccess()
        } else {
            authClient.newCall(
                POST("$API_URL/sync/ratings", body = payload),
            ).awaitSuccess()
        }
    }

    private suspend fun updateProgress(track: Track) {
        // first remove
        authClient.newCall(
            POST("$API_URL/sync/history/remove", body = buildProgressObject(track, false)),
        ).awaitSuccess()
        // then add again
        authClient.newCall(
            POST("$API_URL/sync/history", body = buildProgressObject(track, true)),
        ).awaitSuccess()
    }

    private fun buildProgressObject(track: Track, add: Boolean = true) = buildJsonObject {
        putJsonArray("shows") {
            addJsonObject {
                putJsonObject("ids") {
                    put("simkl", track.remote_id)
                }
                putJsonArray("seasons") {
                    addJsonObject {
                        put("number", 1)
                        if (add) {
                            putJsonArray("episodes") {
                                for (epNum in 1..track.last_episode_seen.toInt()) {
                                    addJsonObject {
                                        put("number", epNum)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }.toString().toRequestBody(jsonMime)

    /**
     * Starts an isolated Simkl rewatch session. This deliberately does not use
     * /sync/history/remove: that endpoint removes the user's canonical watch history.
     */
    suspend fun startRewatch(track: Track): Long {
        requireRewatchEntitlement()
        val response = authClient.newCall(
            POST(
                "$API_URL/sync/history?allow_rewatch=yes",
                body = buildRewatchProgressObject(track, rewatchId = null, includeCompletedEpisodes = true),
            ),
        ).awaitSuccess().let { response ->
            with(json) { response.parseAs<SimklHistoryResponse>() }
        }
        return response.added?.statuses.orEmpty()
            .firstOrNull { it.rewatchId != null }
            ?.rewatchId
            ?: error("Simkl did not create a rewatch session")
    }

    suspend fun updateRewatchProgress(track: Track, rewatchId: Long) {
        if (track.last_episode_seen <= 0.0) return
        authClient.newCall(
            POST(
                "$API_URL/sync/history?allow_rewatch=yes",
                body = buildRewatchProgressObject(track, rewatchId, includeCompletedEpisodes = false),
            ),
        ).awaitSuccess()
    }

    private fun buildRewatchProgressObject(
        track: Track,
        rewatchId: Long?,
        includeCompletedEpisodes: Boolean,
    ) = buildJsonObject {
        val type = track.tracking_url.substringAfter("/").substringBefore("/")
        putJsonArray(if (type == "movies") "movies" else "shows") {
            addJsonObject {
                putJsonObject("ids") {
                    put("simkl", track.remote_id)
                }
                put("is_rewatch", true)
                rewatchId?.let { put("rewatch_id", it) }
                if (type != "movies" && track.last_episode_seen > 0.0) {
                    putJsonArray("seasons") {
                        addJsonObject {
                            put("number", 1)
                            putJsonArray("episodes") {
                                val firstEpisode = if (includeCompletedEpisodes) 1 else track.last_episode_seen.toInt()
                                for (episode in firstEpisode..track.last_episode_seen.toInt()) {
                                    addJsonObject { put("number", episode) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }.toString().toRequestBody(jsonMime)

    suspend fun updateLibAnime(track: Track): Track {
        return withIOContext {
            // determine media type
            val type = track.tracking_url
                .substringAfter("/")
                .substringBefore("/")
            val mediaType = if (type == "movies") "movies" else "shows"
            // update progress only for shows
            if (type != "movies") {
                updateProgress(track)
            }
            // add to correct list
            addToList(track, mediaType)
            // update rating
            updateRating(track, mediaType)

            track
        }
    }

    suspend fun search(search: String, type: String): List<TrackSearch> {
        return withIOContext {
            val searchUrl = "$API_URL/search/$type".toUri().buildUpon()
                .appendQueryParameter("q", search)
                .appendQueryParameter("extended", "full")
                .appendQueryParameter("client_id", CLIENT_ID)
                .build()
            with(json) {
                client.newCall(GET(searchUrl.toString()))
                    .awaitSuccess()
                    .parseAs<List<SimklSearchResult>>()
                    .map { it.toTrackSearch(type) }
            }
        }
    }

    /**
     * Checks if the given [track] exists in the user's list and
     * returns all info about it or null if it isn't found.
     */
    suspend fun findLibAnime(track: Track): Track? {
        return withIOContext {
            val payload = buildJsonArray {
                addJsonObject {
                    put("simkl", track.remote_id)
                }
            }.toString().toRequestBody(jsonMime)
            val foundAnime = with(json) {
                authClient.newCall(
                    POST("$API_URL/sync/watched", body = payload),
                )
                    .awaitSuccess()
                    .parseAs<List<SimklSyncWatched>>()
                    .firstOrNull() ?: return@withIOContext null
            }

            if (foundAnime.result != true) return@withIOContext null
            val lastWatched = foundAnime.lastWatchedAt ?: return@withIOContext null
            val status = foundAnime.list ?: return@withIOContext null
            val type = track.tracking_url
                .substringAfter("/")
                .substringBefore("/")
            val queryType = if (type == "tv") "shows" else type
            val url = "$API_URL/sync/all-items/$queryType/$status".toUri().buildUpon()
                .appendQueryParameter("date_from", lastWatched)
                .build()

            val typeName = if (type == "movies") "movie" else "show"
            val listAnime = with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<SimklSyncResult?>()
                    ?.getFromType(queryType)
                    ?.firstOrNull { item ->
                        item.getFromType(typeName).ids.simkl == track.remote_id
                    } ?: return@withIOContext null
            }

            val activeRewatch = findActiveRewatch(track, queryType, typeName, lastWatched)
            (activeRewatch ?: listAnime).toTrack(
                typeName = typeName,
                type = type,
                statusString = if (activeRewatch != null) "rewatching" else status,
            )
        }
    }

    suspend fun findActiveRewatchId(track: Track): Long? {
        val type = track.tracking_url.substringAfter("/").substringBefore("/")
        val queryType = if (type == "tv") "shows" else type
        return findActiveRewatch(track, queryType, if (type == "movies") "movie" else "show", null)?.rewatchId
    }

    private suspend fun findActiveRewatch(
        track: Track,
        queryType: String,
        typeName: String,
        dateFrom: String?,
    ): eu.kanade.tachiyomi.data.track.simkl.dto.SimklSyncItem? {
        val url = "$API_URL/sync/all-items/$queryType/all".toUri().buildUpon()
            .appendQueryParameter("allow_rewatch", "yes")
            .appendQueryParameter("extended", "full")
            .apply { dateFrom?.let { appendQueryParameter("date_from", it) } }
            .build()
        return with(json) {
            authClient.newCall(GET(url.toString()))
                .awaitSuccess()
                .parseAs<SimklSyncResult>()
                .getFromType(queryType)
                .orEmpty()
                .firstOrNull { item ->
                    item.isRewatch &&
                        item.rewatchStatus == "active" &&
                        item.getFromType(typeName).ids.simkl == track.remote_id
                }
        }
    }

    suspend fun isRewatchEligible(): Boolean {
        return getCurrentUser().account.type?.lowercase() in setOf("pro", "vip")
    }

    private suspend fun requireRewatchEntitlement() {
        check(isRewatchEligible()) { "Simkl rewatch tracking requires a Simkl PRO or VIP account" }
    }

    suspend fun getCurrentUser(): SimklUser {
        return withIOContext {
            with(json) {
                authClient.newCall(GET("$API_URL/users/settings"))
                    .awaitSuccess()
                    .parseAs()
            }
        }
    }

    suspend fun getPaginatedMangaList(page: Int, status: String): List<TrackMangaMetadata> {
        val url = "$API_URL/sync/all-items/anime/$status".toUri().buildUpon()
            .appendQueryParameter("page", page.toString())
            .appendQueryParameter("limit", PAGE_SIZE.toString())
            .build()
        return withIOContext {
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<SimklSyncResult>()
                    .getFromType("anime")
                    .orEmpty()
                    .map { item ->
                        item.getFromType("show").let {
                            TrackMangaMetadata(remoteId = it.ids.simkl, title = it.title)
                        }
                    }
            }
        }
    }

    suspend fun getSimklAnimeMetadata(track: DomainTrack): TrackMangaMetadata {
        return withIOContext {
            val type = track.remoteUrl
                .substringAfter("/")
                .substringBefore("/")
            val url = "$API_URL/$type/${track.remoteId}?extended=full&client_id=$CLIENT_ID"
            with(json) {
                authClient.newCall(GET(url))
                    .awaitSuccess()
                    .parseAs<SimklAnimeResponse>()
                    .let { anime ->
                        TrackMangaMetadata(
                            remoteId = anime.id,
                            title = anime.title,
                            thumbnailUrl = anime.poster?.let { "$POSTERS_URL${it}_m.webp" },
                            description = anime.overview,
                            authors = when (type) {
                                "anime" -> anime.studios?.joinToString { it.name }
                                "movies" -> anime.director
                                else -> anime.network
                            },
                            artists = anime.network,
                            genres = anime.genres?.filter(String::isNotBlank),
                            status = when (anime.status?.lowercase()) {
                                "returning series", "continuing", "in production" ->
                                    eu.kanade.tachiyomi.source.model.SManga.ONGOING.toLong()
                                "ended", "released" -> eu.kanade.tachiyomi.source.model.SManga.COMPLETED.toLong()
                                "canceled", "cancelled" -> eu.kanade.tachiyomi.source.model.SManga.CANCELLED.toLong()
                                "hiatus" -> eu.kanade.tachiyomi.source.model.SManga.ON_HIATUS.toLong()
                                else -> null
                            },
                        )
                    }
            }
        }
    }

    suspend fun accessToken(code: String): SimklOAuth {
        return withIOContext {
            with(json) {
                client.newCall(accessTokenRequest(code))
                    .awaitSuccess()
                    .parseAs()
            }
        }
    }

    private fun accessTokenRequest(code: String) = POST(
        OAUTH_URL,
        body = buildJsonObject {
            put("code", code)
            put("client_id", CLIENT_ID)
            put("client_secret", CLIENT_SECRET)
            put("redirect_uri", REDIRECT_URL)
            put("grant_type", "authorization_code")
        }.toString().toRequestBody(jsonMime),
    )

    companion object {
        const val CLIENT_ID = "f9911f4e260444a04e4939713995902a0f8168f57f548bda1907fc7196de6673"
        private const val CLIENT_SECRET = "6403240b6a96fb1f5aeccb8f6fec39b02810195e378a76d43b5088465ce6f62e"

        private const val BASE_URL = "https://simkl.com"
        private const val API_URL = "https://api.simkl.com"
        private const val OAUTH_URL = "$API_URL/oauth/token"
        private const val LOGIN_URL = "$BASE_URL/oauth/authorize"
        const val POSTERS_URL = "https://simkl.in/posters/"

        private const val REDIRECT_URL = "anikku://simkl-auth"
        private const val PAGE_SIZE = 50

        fun authUrl(): Uri =
            LOGIN_URL.toUri().buildUpon()
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("redirect_uri", REDIRECT_URL)
                .build()
    }
}

@Serializable
private data class SimklHistoryResponse(
    val added: SimklHistoryAdded? = null,
)

@Serializable
private data class SimklHistoryAdded(
    val statuses: List<SimklHistoryStatus> = emptyList(),
)

@Serializable
private data class SimklHistoryStatus(
    @SerialName("rewatch_id") val rewatchId: Long? = null,
)
