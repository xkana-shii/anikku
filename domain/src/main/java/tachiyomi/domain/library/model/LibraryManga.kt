package tachiyomi.domain.library.model

import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.manga.model.Manga

data class LibraryManga(
    val manga: Manga,
    val categories: List<Long>,
    val totalChapters: Long,
    val readCount: Long,
    val bookmarkCount: Long,
    // KMK -->
    val bookmarkReadCount: Long,
    val chapterFlags: Long,
    // KMK <--
    // AY -->
    val fillermarkCount: Long,
    val fillermarkReadCount: Long,
    val fillermarkBookmarkCount: Long,
    val fillermarkBookmarkReadCount: Long,
    // <-- AY
    val latestUpload: Long,
    val chapterFetchedAt: Long,
    val lastRead: Long,
) {
    val id: Long = manga.id

    val unreadCount get() = unreadCount(TriState.DISABLED)

    fun unreadCount(fillermarkedFilter: TriState): Long {
        val (total, read, bookmarks, bookmarkRead) = when (fillermarkedFilter) {
            TriState.ENABLED_IS -> listOf(
                fillermarkCount,
                fillermarkReadCount,
                fillermarkBookmarkCount,
                fillermarkBookmarkReadCount,
            )
            TriState.ENABLED_NOT -> listOf(
                totalChapters - fillermarkCount,
                readCount - fillermarkReadCount,
                bookmarkCount - fillermarkBookmarkCount,
                bookmarkReadCount - fillermarkBookmarkReadCount,
            )
            TriState.DISABLED -> listOf(totalChapters, readCount, bookmarkCount, bookmarkReadCount)
        }
        return when {
            chapterFlags and Manga.EPISODE_SHOW_NOT_BOOKMARKED != 0L -> total - bookmarks - (read - bookmarkRead)
            chapterFlags and Manga.EPISODE_SHOW_BOOKMARKED != 0L -> bookmarks - bookmarkRead
            else -> total - read
        }
    }

    fun filteredEpisodeCount(fillermarkedFilter: TriState): Long = when (fillermarkedFilter) {
        TriState.ENABLED_IS -> fillermarkCount
        TriState.ENABLED_NOT -> totalChapters - fillermarkCount
        TriState.DISABLED -> totalChapters
    }

    val hasBookmarks
        get() = bookmarkCount > 0

    val hasStarted = readCount > 0
}
