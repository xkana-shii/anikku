package eu.kanade.tachiyomi.animesource

import eu.kanade.tachiyomi.animesource.model.SAnime

/** Optional typed metadata contract for exact tracker binding. */
interface TrackerIdMetadataSource {
    suspend fun getTrackerIdMetadata(anime: SAnime): TrackerIdMetadata?
}

data class TrackerIdMetadata(
    val aniListId: String? = null,
    val myAnimeListId: String? = null,
    val kitsuId: String? = null,
)
