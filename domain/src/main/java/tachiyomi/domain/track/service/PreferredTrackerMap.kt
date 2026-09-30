package tachiyomi.domain.track.service

import kotlinx.serialization.json.Json

/** Bounded, sync-safe per-entry tracker overrides. */
object PreferredTrackerMap {
    private const val MAX_ENTRIES = 5_000
    private const val MAX_LENGTH = 256_000

    fun decode(raw: String): Map<Long, Long> = runCatching {
        if (raw.length > MAX_LENGTH) {
            emptyMap()
        } else {
            Json.decodeFromString<Map<String, Long>>(raw).entries.asSequence()
                .mapNotNull { (id, tracker) -> id.toLongOrNull()?.takeIf { it >= 0 && tracker > 0 }?.let { it to tracker } }
                .take(MAX_ENTRIES)
                .toMap()
        }
    }.getOrDefault(emptyMap())

    fun update(raw: String, animeId: Long, trackerId: Long?): String {
        val values = decode(raw).toMutableMap()
        values.remove(animeId)
        if (animeId >= 0 && trackerId != null && trackerId > 0) values[animeId] = trackerId
        return Json.encodeToString(values.entries.toList().takeLast(MAX_ENTRIES).associate { it.key.toString() to it.value })
    }
}
