package eu.kanade.tachiyomi.data.webhook

import eu.kanade.domain.connections.service.WebhookEvent
import eu.kanade.domain.connections.service.WebhookPreferences
import eu.kanade.domain.source.interactor.GetIncognitoState
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import exh.log.xLogE
import exh.source.MERGED_SOURCE_ID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaMergeRepository
import tachiyomi.domain.track.interactor.GetTracks
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Isolated bounded delivery queue. Calling [notify] is intentionally never a
 * suspend operation, so a webhook failure cannot fail playback, downloads, or
 * a database operation.
 */
class WebhookNotifier(
    private val preferences: WebhookPreferences,
    private val incognito: GetIncognitoState,
    private val categories: GetCategories,
    private val network: NetworkHelper,
    private val tracks: GetTracks = Injekt.get(),
    private val mangaMergeRepository: MangaMergeRepository = Injekt.get(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private data class Message(val event: WebhookEvent, val anime: Manga?, val data: Map<String, String>)
    private val queue = Channel<Message>(64)

    init {
        scope.launch {
            for (message in queue) {
                try {
                    if (enabled(message.event) && !suppressed(message.anime)) send(message)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    "WebhookNotifier".xLogE("Webhook delivery failed: ${e.javaClass.simpleName}")
                }
            }
        }
    }

    fun notify(event: WebhookEvent, anime: Manga? = null, data: Map<String, String> = emptyMap()) {
        try {
            if (enabled(event) && !incognito.await(anime?.source) && queue.trySend(Message(event, anime, data.toMap())).isFailure) {
                "WebhookNotifier".xLogE("Webhook queue full; event dropped")
            }
        } catch (e: Exception) {
            "WebhookNotifier".xLogE("Webhook enqueue failed: ${e.javaClass.simpleName}")
        }
    }

    suspend fun sendTest() {
        check(preferences.enabled().get()) { "Webhooks are disabled" }
        send(Message(WebhookEvent.TEST, null, mapOf("message" to "Anikku webhook test")))
    }

    private fun enabled(event: WebhookEvent): Boolean = preferences.enabled().get() &&
        (event == WebhookEvent.TEST || preferences.events().get().contains(event.id) || preferences.event(event).get())

    private suspend fun suppressed(anime: Manga?): Boolean {
        if (incognito.await(anime?.source)) return true
        if (anime == null) return false
        if (anime.source == MERGED_SOURCE_ID) {
            val children = mangaMergeRepository.getMergedMangaById(anime.id)
            if (children.any { incognito.await(it.source) }) return true
        }
        val categoryIds = categories.await(anime.id).map { it.id.toString() }.ifEmpty { listOf("0") }
        return categoryIds.any { it in preferences.excludedCategories().get() } ||
            tracks.await(anime.id).any { it.private }
    }

    private suspend fun send(message: Message) {
        val data = message.anime?.let { mapOf("anime" to it.title) }.orEmpty() + message.data
        val cover = message.anime?.thumbnailUrl?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
        val timestamp = Instant.now().toString()
        val destinations = listOf(
            preferences.discordUrl().get() to WebhookPayload.discord(message.event, data, timestamp, cover),
            preferences.genericUrl().get() to WebhookPayload.generic(message.event, data, timestamp, cover),
        ).filter { it.first.isNotBlank() }
        check(destinations.isNotEmpty()) { "No webhook URL configured" }
        var failure: Exception? = null
        destinations.forEach { (url, payload) ->
            try {
                network.client.newBuilder().callTimeout(15, TimeUnit.SECONDS).build()
                    .newCall(POST(url, body = payload.toString().toRequestBody(jsonMime))).awaitSuccess().use { }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failure = e
                "WebhookNotifier".xLogE("Webhook request failed: ${e.javaClass.simpleName}")
            }
        }
        failure?.let { throw it }
    }
}
