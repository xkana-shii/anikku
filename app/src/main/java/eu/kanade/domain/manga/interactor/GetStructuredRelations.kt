package eu.kanade.domain.manga.interactor

import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.track.TrackerManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.StructuredRelation
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.model.Track

class GetStructuredRelations(
    private val trackerManager: TrackerManager,
    private val trackPreferences: TrackPreferences,
    private val mangaRepository: MangaRepository,
    private val getTracks: GetTracks,
) {
    private data class Key(val animeId: Long, val preferred: Long?, val bindings: List<Pair<Long, Long>>)
    private data class Cached(val time: Long, val entries: List<StructuredRelation>)

    private val cache = linkedMapOf<Key, Cached>()
    private val mutex = Mutex()

    suspend fun await(
        anime: Manga,
        tracks: List<Track>,
        refresh: Boolean = false,
    ): List<StructuredRelation> {
        val applicable = tracks.filter { track ->
            trackerManager.get(track.trackerId)?.let { it.isLoggedIn && it.supportsStructuredRelations } == true
        }
        if (applicable.isEmpty()) return emptyList()
        val preferred = trackPreferences.resolvePreferredTracker(anime.id, applicable.map { it.trackerId }.toSet())
        val key = Key(anime.id, preferred, applicable.map { it.trackerId to it.remoteId }.sortedBy { it.first })
        val entries = mutex.withLock {
            val cached = cache[key]?.takeIf {
                !refresh && System.currentTimeMillis() - it.time < CACHE_DURATION
            }
            cached?.entries ?: fetch(applicable.sortedBy { it.trackerId != preferred }).also {
                if (it.isEmpty()) {
                    cache.remove(key)
                } else {
                    cache[key] = Cached(System.currentTimeMillis(), it)
                    if (cache.size > MAX_CACHE_SIZE) cache.remove(cache.keys.first())
                }
            }
        }
        return resolveLocally(anime, entries)
    }

    private suspend fun fetch(tracks: List<Track>): List<StructuredRelation> {
        var failure: Exception? = null
        for (track in tracks) {
            try {
                trackerManager.get(track.trackerId)?.getStructuredRelations(track.remoteId)
                    .orEmpty()
                    .takeIf { it.isNotEmpty() }
                    ?.let { return normalize(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failure = e
            }
        }
        failure?.let { throw it }
        return emptyList()
    }

    private suspend fun resolveLocally(
        anime: Manga,
        entries: List<StructuredRelation>,
    ): List<StructuredRelation> {
        if (entries.isEmpty()) return emptyList()
        val favorites = mangaRepository.getFavorites()
        val allTracks = getTracks.await()
        return entries.map { entry ->
            val bound = allTracks
                .filter { it.trackerId == entry.trackerId && it.remoteId == entry.remoteId }
                .sortedByDescending { track -> favorites.any { it.id == track.mangaId } }
                .firstOrNull()
            val sourceMatch = entry.sourceUrl?.let {
                mangaRepository.getMangaByUrlAndSourceId(it, anime.source)
            }
            val titled = favorites.filter { it.title.equals(entry.title, ignoreCase = true) }
            val local = bound?.mangaId
                ?: sourceMatch?.id
                ?: titled.filter { it.source == anime.source }.singleOrNull()?.id
                ?: titled.singleOrNull()?.id
            entry.copy(localAnimeId = local?.takeIf { it != anime.id })
        }
    }

    private fun normalize(entries: List<StructuredRelation>) = entries
        .filter { it.title.isNotBlank() && (it.url != null || it.remoteId != null) }
        .distinctBy { listOf(it.trackerId, it.remoteId, it.url, it.relation) }

    private companion object {
        const val CACHE_DURATION = 24 * 60 * 60 * 1000L
        const val MAX_CACHE_SIZE = 100
    }
}
