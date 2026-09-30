package eu.kanade.tachiyomi.data.track.simkl

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.data.track.simkl.dto.SimklOAuth
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.Json
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.ConcurrentHashMap
import tachiyomi.domain.track.model.Track as DomainTrack

class Simkl(id: Long) : BaseTracker(id, "Simkl") {

    companion object {
        const val WATCHING = 1L
        const val COMPLETED = 2L
        const val ON_HOLD = 3L
        const val NOT_INTERESTING = 4L
        const val PLAN_TO_WATCH = 5L
        const val REWATCHING = 6L

        private val SCORE_LIST = IntRange(0, 10)
            .map(Int::toString)
            .toImmutableList()
    }

    private val json: Json by injectLazy()

    private val interceptor by lazy { SimklInterceptor(this) }

    private val api by lazy { SimklApi(client, interceptor) }

    /** Active session ids are re-discovered from Simkl after an app restart when needed. */
    private val activeRewatchIds = ConcurrentHashMap<Long, Long>()

    override fun getScoreList(): ImmutableList<String> = SCORE_LIST

    override fun displayScore(track: DomainTrack): String {
        return track.score.toInt().toString()
    }

    private suspend fun add(track: Track): Track {
        return api.addLibAnime(track)
    }

    override suspend fun update(track: Track, didReadChapter: Boolean): Track {
        if (track.status == REWATCHING) {
            val rewatchId = activeRewatchIds[track.remote_id] ?: api.startRewatch(track).also {
                activeRewatchIds[track.remote_id] = it
            }
            if (didReadChapter) {
                api.updateRewatchProgress(track, rewatchId)
            }
            return track
        }

        // BaseTracker changes the local status to completed for the last episode. Keep using an
        // existing Simkl rewatch session so that completion cannot overwrite canonical history.
        val activeRewatchId = activeRewatchIds[track.remote_id] ?: api.findActiveRewatchId(track)
        if (activeRewatchId != null) {
            api.updateRewatchProgress(track, activeRewatchId)
            if (track.total_episodes > 0 && track.last_episode_seen.toLong() == track.total_episodes) {
                activeRewatchIds.remove(track.remote_id)
            } else {
                activeRewatchIds[track.remote_id] = activeRewatchId
            }
            return track
        }

        if (track.status != COMPLETED) {
            if (didReadChapter) {
                if (track.last_episode_seen.toLong() == track.total_episodes && track.total_episodes > 0) {
                    track.status = COMPLETED
                } else {
                    track.status = WATCHING
                }
            }
        }

        return api.updateLibAnime(track)
    }

    override suspend fun bind(track: Track, hasReadChapters: Boolean): Track {
        val remoteTrack = api.findLibAnime(track)
        return if (remoteTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            track.library_id = remoteTrack.library_id

            if (track.status != COMPLETED) {
                track.status = if (hasReadChapters) WATCHING else track.status
            }

            update(track)
        } else {
            // Set default fields if it's not found in the list
            track.status = if (hasReadChapters) WATCHING else PLAN_TO_WATCH
            track.score = 0.0
            add(track)
        }
    }

    override suspend fun search(query: String): List<TrackSearch> {
        return api.search(query, "anime") +
            api.search(query, "tv") +
            api.search(query, "movie")
    }

    override suspend fun refresh(track: Track): Track {
        api.findLibAnime(track)?.let { remoteTrack ->
            track.copyPersonalFrom(remoteTrack)
            track.total_episodes = remoteTrack.total_episodes
        }
        return track
    }

    override fun getLogo() = R.drawable.brand_simkl

    override fun getStatusList(): List<Long> {
        return listOf(WATCHING, COMPLETED, ON_HOLD, NOT_INTERESTING, PLAN_TO_WATCH, REWATCHING)
    }

    override fun getStatus(status: Long): StringResource? = when (status) {
        WATCHING -> AYMR.strings.watching
        PLAN_TO_WATCH -> AYMR.strings.plan_to_watch
        COMPLETED -> MR.strings.completed
        ON_HOLD -> MR.strings.on_hold
        NOT_INTERESTING -> AYMR.strings.not_interesting
        REWATCHING -> AYMR.strings.repeating_anime
        else -> null
    }

    override fun getReadingStatus(): Long = WATCHING

    override fun getRereadingStatus(): Long = REWATCHING

    override fun getCompletionStatus(): Long = COMPLETED

    override suspend fun login(username: String, password: String) = login(password)

    suspend fun login(code: String) {
        try {
            val oauth = api.accessToken(code)
            interceptor.newAuth(oauth)
            val user = api.getCurrentUser()
            saveCredentials(user.account.id.toString(), oauth.accessToken)
        } catch (_: Throwable) {
            logout()
        }
    }

    fun saveToken(oauth: SimklOAuth?) {
        trackPreferences.trackToken(this).set(json.encodeToString(oauth))
    }

    override suspend fun getMangaMetadata(track: DomainTrack): TrackMangaMetadata {
        return api.getSimklAnimeMetadata(track)
    }

    suspend fun canTrackRewatches(): Boolean = api.isRewatchEligible()

    override suspend fun getPaginatedMangaList(page: Int, statusId: Long): List<TrackMangaMetadata> {
        if (statusId == REWATCHING) return emptyList()
        return api.getPaginatedMangaList(page, statusId.toSimklListStatus())
    }

    fun restoreToken(): SimklOAuth? {
        return try {
            json.decodeFromString<SimklOAuth>(trackPreferences.trackToken(this).get())
        } catch (_: Exception) {
            null
        }
    }

    override fun logout() {
        super.logout()
        trackPreferences.trackToken(this).delete()
        interceptor.newAuth(null)
    }

    // KMK -->
    override fun hasNotStartedReading(status: Long): Boolean = status == PLAN_TO_WATCH
    // KMK <--
}
