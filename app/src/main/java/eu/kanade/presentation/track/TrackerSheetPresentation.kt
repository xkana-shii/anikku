package eu.kanade.presentation.track

import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.ui.manga.track.TrackItem
import eu.kanade.tachiyomi.util.lang.toLocalDate
import tachiyomi.domain.track.service.TrackerProgressSync
import tachiyomi.i18n.MR
import java.time.format.DateTimeFormatter

internal val TrackItem.canChangeEntry: Boolean get() = tracker !is EnhancedTracker

internal enum class UnifiedTrackerIconAction { OPEN, SEARCH, COPY_LINK, SET_PREFERRED, NONE }

internal fun TrackItem.unifiedIconClickAction(editMode: Boolean): UnifiedTrackerIconAction = when {
    track == null -> UnifiedTrackerIconAction.SEARCH
    !editMode -> UnifiedTrackerIconAction.OPEN
    canChangeEntry -> UnifiedTrackerIconAction.SEARCH
    else -> UnifiedTrackerIconAction.NONE
}

internal fun TrackItem.unifiedIconLongPressAction(editMode: Boolean): UnifiedTrackerIconAction = when {
    track == null -> UnifiedTrackerIconAction.NONE
    editMode -> UnifiedTrackerIconAction.SET_PREFERRED
    else -> UnifiedTrackerIconAction.COPY_LINK
}

internal class TrackerSheetPresentation(
    items: List<TrackItem>,
    preferredId: Long?,
    dateFormat: DateTimeFormatter,
    errorTrackerIds: Set<Long>,
) {
    val bound = items.filter { it.track != null }
    val visibleItems = items.filter { it.track != null || it.canChangeEntry }
    private val tracks = bound.mapNotNull { it.track }
    private val resolved = TrackerProgressSync.resolvePreferredTrack(tracks, preferredId)
    val primary = bound.find { it.track == resolved }
    val scoreItem = bound.firstOrNull { it.tracker.getScoreList().isNotEmpty() }
    val dateItem = bound.firstOrNull { it.tracker.supportsReadingDates }
    val supportsScore = scoreItem != null
    val supportsReadingDates = dateItem != null
    val status = primary?.let { it.tracker.getStatus(it.track!!.status) } ?: MR.strings.reading
    val score = scoreItem?.let { item ->
        item.track!!.takeIf { it.score != 0.0 }?.let(item.tracker::displayScore)?.takeIf(String::isNotBlank)
    }
    val appendScoreStar = score != null && '★' !in score
    val startDate = dateItem?.track?.startDate?.takeIf { it != 0L }?.let { dateFormat.format(it.toLocalDate()) }
    val finishDate = dateItem?.track?.finishDate?.takeIf { it != 0L }?.let { dateFormat.format(it.toLocalDate()) }
    val mismatchedIds = TrackerProgressSync.mismatchedIds(tracks, preferredId)
    val errorIds = bound.map { it.tracker.id }.toSet() intersect errorTrackerIds
}
