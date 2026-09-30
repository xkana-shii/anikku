package eu.kanade.tachiyomi.data.webhook

import eu.kanade.domain.connections.service.WebhookEvent
import eu.kanade.domain.connections.service.WebhookPreferences
import eu.kanade.domain.source.interactor.GetIncognitoState
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import logcat.LogPriority
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.manga.model.Manga
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
                    logcat(LogPriority.WARN, e) { "Webhook delivery failed" }
                }
            }
        }
    }

    fun notify(event: WebhookEvent, anime: Manga? = null, data: Map<String, String> = emptyMap()) {
        try {
            if (enabled(event) && !incognito.await(anime?.source)) queue.trySend(Message(event, anime, data.toMap()))
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Webhook enqueue failed" }
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
        return categories.await(anime.id).map { it.id.toString() }.ifEmpty { listOf("0") }
            .any { it in preferences.excludedCategories().get() }
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
        destinations.forEach { (url, payload) ->
            network.client.newBuilder().callTimeout(15, TimeUnit.SECONDS).build()
                .newCall(POST(url, body = payload.toString().toRequestBody(jsonMime))).awaitSuccess().use { }
        }
    }
}
