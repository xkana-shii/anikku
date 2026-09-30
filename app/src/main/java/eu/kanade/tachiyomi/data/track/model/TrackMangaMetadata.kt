package eu.kanade.tachiyomi.data.track.model

data class TrackMangaMetadata(
    val remoteId: Long? = null,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val description: String? = null,
    val authors: String? = null,
    val artists: String? = null,
    val genres: List<String>? = null,
    /** Source status value when the tracker can map it safely to Anikku's status model. */
    val status: Long? = null,
)
