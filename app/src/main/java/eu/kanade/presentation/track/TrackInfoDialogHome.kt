package eu.kanade.presentation.track

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.track.components.TrackLogoIcon
import eu.kanade.presentation.util.isTvUi
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.ui.manga.track.TrackItem
import eu.kanade.tachiyomi.util.lang.toLocalDate
import eu.kanade.tachiyomi.util.system.copyToClipboard
import tachiyomi.domain.track.service.TrackerProgressSync
import tachiyomi.i18n.MR
import tachiyomi.i18n.ank.AMR
import tachiyomi.presentation.core.i18n.stringResource
import java.time.format.DateTimeFormatter

@Composable
fun TrackInfoDialogHome(
    trackItems: List<TrackItem>,
    dateFormat: DateTimeFormatter,
    seriesTitle: String = "",
    // AM -->
    isSeason: Boolean,
    // <-- AM
    isActive: Boolean = true,
    onStatusClick: (TrackItem) -> Unit,
    onChapterClick: (TrackItem) -> Unit,
    onScoreClick: (TrackItem) -> Unit,
    onStartDateEdit: (TrackItem) -> Unit,
    onEndDateEdit: (TrackItem) -> Unit,
    onNewSearch: (TrackItem) -> Unit,
    onOpenInBrowser: (TrackItem) -> Unit,
    onRemoved: (TrackItem) -> Unit,
    onCopyLink: (TrackItem) -> Unit,
    onTogglePrivate: (TrackItem) -> Unit,
    preferredId: Long? = null,
    editMode: Boolean = false,
    onToggleEditMode: () -> Unit = {},
    onSetPreferredTracker: (TrackItem) -> Unit = {},
    selectedTrackerIds: Set<Long> = emptySet(),
    onToggleTrackerSelection: (TrackItem) -> Unit = {},
    onRemoveSelectedTrackers: () -> Unit = {},
    onAdjustProgress: (Int) -> Unit = {},
    onRemoveTracking: (List<TrackItem>) -> Unit = {},
    skippedTrackerIds: Set<Long> = emptySet(),
    errorTrackerIds: Set<Long> = emptySet(),
    busy: Boolean = false,
) {
    val initialFocusRequester = remember { FocusRequester() }
    val isTvUi = isTvUi()
    LaunchedEffect(isTvUi, isActive, trackItems.isNotEmpty()) {
        if (isTvUi && isActive && trackItems.isNotEmpty()) {
            withFrameNanos { }
            initialFocusRequester.requestFocus()
        }
    }
    Column(
        modifier = Modifier
            .animateContentSize()
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.systemBars),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
            if (editMode && selectedTrackerIds.isNotEmpty()) {
                IconButton(onClick = onRemoveSelectedTrackers) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(AMR.strings.tracker_remove_selected),
                    )
                }
            }
            IconButton(onClick = onToggleEditMode) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(AMR.strings.tracker_edit_mode),
                    tint = if (editMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        val boundItems = trackItems.filter { it.track != null }
        if (boundItems.size >= 2) {
            UnifiedTrackerCard(
                items = trackItems,
                initialFocusRequester = initialFocusRequester,
                dateFormat = dateFormat,
                seriesTitle = seriesTitle,
                preferredId = preferredId,
                editMode = editMode,
                busy = busy,
                selectedTrackerIds = selectedTrackerIds,
                skippedTrackerIds = skippedTrackerIds,
                errorTrackerIds = errorTrackerIds,
                onToggleEditMode = onToggleEditMode,
                onToggleTrackerSelection = onToggleTrackerSelection,
                onSetPreferredTracker = onSetPreferredTracker,
                onNewSearch = onNewSearch,
                onOpenInBrowser = onOpenInBrowser,
                onCopyLink = onCopyLink,
                onRemoved = onRemoved,
                onRemoveSelectedTrackers = onRemoveSelectedTrackers,
                onRemoveTracking = onRemoveTracking,
                onTogglePrivate = onTogglePrivate,
                onStatusClick = onStatusClick,
                onChapterClick = onChapterClick,
                onScoreClick = onScoreClick,
                onStartDateEdit = onStartDateEdit,
                onEndDateEdit = onEndDateEdit,
                onAdjustProgress = onAdjustProgress,
            )
        } else {
            trackItems.forEachIndexed { index, item ->
                val initialFocusModifier = if (index == 0) {
                    Modifier.focusRequester(initialFocusRequester)
                } else {
                    Modifier
                }
                if (item.track != null) {
                    val supportsScoring = item.tracker.getScoreList().isNotEmpty()
                    val supportsReadingDates = item.tracker.supportsReadingDates
                    val supportsPrivate = item.tracker.supportsPrivateTracking
                    TrackInfoItem(
                        title = item.track.title,
                        tracker = item.tracker,
                        initialFocusModifier = initialFocusModifier,
                        // AM -->
                        isSeason = isSeason,
                        // <-- AM
                        status = item.tracker.getStatus(item.track.status),
                        onStatusClick = { onStatusClick(item) },
                        chapters = "${item.track.lastChapterRead.toInt()}".let {
                            val totalChapters = item.track.totalChapters
                            if (totalChapters > 0) {
                                // Add known total chapter count
                                "$it / $totalChapters"
                            } else {
                                it
                            }
                        },
                        onChaptersClick = { onChapterClick(item) },
                        score = item.tracker.displayScore(item.track)
                            .takeIf { supportsScoring && item.track.score != 0.0 },
                        onScoreClick = { onScoreClick(item) }
                            .takeIf { supportsScoring },
                        startDate = remember(item.track.startDate) { dateFormat.format(item.track.startDate.toLocalDate()) }
                            .takeIf { supportsReadingDates && item.track.startDate != 0L },
                        onStartDateClick = { onStartDateEdit(item) } // TODO
                            .takeIf { supportsReadingDates },
                        endDate = dateFormat.format(item.track.finishDate.toLocalDate())
                            .takeIf { supportsReadingDates && item.track.finishDate != 0L },
                        onEndDateClick = { onEndDateEdit(item) }
                            .takeIf { supportsReadingDates },
                        onNewSearch = { onNewSearch(item) },
                        onOpenInBrowser = { onOpenInBrowser(item) },
                        onRemoved = { onRemoved(item) },
                        onCopyLink = { onCopyLink(item) },
                        preferred = item.tracker.id == preferredId,
                        editMode = editMode,
                        onSetPreferred = { onSetPreferredTracker(item) },
                        selected = item.tracker.id in selectedTrackerIds,
                        onToggleSelected = { onToggleTrackerSelection(item) },
                        skipped = item.tracker.id in skippedTrackerIds,
                        syncError = item.tracker.id in errorTrackerIds,
                        private = item.track.private,
                        onTogglePrivate = { onTogglePrivate(item) }
                            .takeIf { supportsPrivate },
                    )
                } else {
                    TrackInfoItemEmpty(
                        tracker = item.tracker,
                        onNewSearch = { onNewSearch(item) },
                        initialFocusModifier = initialFocusModifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun UnifiedTrackerCard(
    items: List<TrackItem>,
    initialFocusRequester: FocusRequester,
    dateFormat: DateTimeFormatter,
    seriesTitle: String,
    preferredId: Long?,
    editMode: Boolean,
    busy: Boolean,
    selectedTrackerIds: Set<Long>,
    skippedTrackerIds: Set<Long>,
    errorTrackerIds: Set<Long>,
    onToggleEditMode: () -> Unit,
    onToggleTrackerSelection: (TrackItem) -> Unit,
    onSetPreferredTracker: (TrackItem) -> Unit,
    onNewSearch: (TrackItem) -> Unit,
    onOpenInBrowser: (TrackItem) -> Unit,
    onCopyLink: (TrackItem) -> Unit,
    onRemoved: (TrackItem) -> Unit,
    onRemoveSelectedTrackers: () -> Unit,
    onRemoveTracking: (List<TrackItem>) -> Unit,
    onTogglePrivate: (TrackItem) -> Unit,
    onStatusClick: (TrackItem) -> Unit,
    onChapterClick: (TrackItem) -> Unit,
    onScoreClick: (TrackItem) -> Unit,
    onStartDateEdit: (TrackItem) -> Unit,
    onEndDateEdit: (TrackItem) -> Unit,
    onAdjustProgress: (Int) -> Unit,
) {
    val bound = items.filter { it.track != null }
    val tracks = bound.mapNotNull { it.track }
    val primaryTrack = TrackerProgressSync.resolvePreferredTrack(tracks, preferredId) ?: return
    val primary = bound.first { it.track == primaryTrack }
    val scoreItem = bound.firstOrNull { it.tracker.getScoreList().isNotEmpty() }
    val dateItem = bound.firstOrNull { it.tracker.supportsReadingDates }
    val mismatchedIds = TrackerProgressSync.mismatchedIds(tracks, preferredId)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = seriesTitle,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (editMode && selectedTrackerIds.isNotEmpty()) {
                IconButton(onClick = onRemoveSelectedTrackers, enabled = !busy) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(AMR.strings.tracker_remove_selected))
                }
            }
            IconButton(
                onClick = onToggleEditMode,
                enabled = !busy,
                modifier = Modifier.focusRequester(initialFocusRequester),
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = stringResource(AMR.strings.tracker_edit_mode),
                    tint = if (editMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        items.forEach { item ->
            if (item.track != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BadgedBox(
                        badge = {
                            val marker = when (item.tracker.id) {
                                in errorTrackerIds -> "!"
                                in skippedTrackerIds -> "–"
                                in selectedTrackerIds -> "✓"
                                in mismatchedIds -> "•"
                                preferredId -> "★"
                                else -> ""
                            }
                            if (marker.isNotEmpty()) Badge { Text(marker) }
                        },
                    ) {
                        TrackLogoIcon(
                            tracker = item.tracker,
                            onClick = { if (editMode) onToggleTrackerSelection(item) else onOpenInBrowser(item) },
                            onLongClick = { if (editMode) onSetPreferredTracker(item) else onCopyLink(item) },
                        )
                    }
                    Text(
                        text = item.track.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).clickable { onNewSearch(item) }.padding(start = 12.dp),
                    )
                    TrackInfoItemMenu(
                        onOpenInBrowser = { onOpenInBrowser(item) },
                        onRemoved = { onRemoved(item) },
                        onCopyLink = { onCopyLink(item) },
                        private = item.track.private,
                        onTogglePrivate = { onTogglePrivate(item) }.takeIf { item.tracker.supportsPrivateTracking },
                    )
                }
            } else {
                TrackInfoItemEmpty(item.tracker, { onNewSearch(item) }, Modifier)
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(primary.tracker.getStatus(primaryTrack.status) ?: MR.strings.reading),
                    modifier = Modifier.weight(1f).clickable(enabled = !busy) { onStatusClick(primary) }.padding(8.dp),
                )
                scoreItem?.let { item ->
                    VerticalDivider()
                    Text(
                        text = item.tracker.displayScore(item.track!!).ifBlank { stringResource(MR.strings.score) },
                        modifier = Modifier.weight(1f).clickable(enabled = !busy) { onScoreClick(item) }.padding(8.dp),
                    )
                }
            }
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("−", modifier = Modifier.clickable(enabled = !busy) { onAdjustProgress(-1) }.padding(12.dp))
                VerticalDivider()
                Text(
                    text = "${primaryTrack.lastChapterRead.toInt()}".let { progress ->
                        if (primaryTrack.totalChapters > 0) "$progress/${primaryTrack.totalChapters}" else progress
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).clickable(enabled = !busy) { onChapterClick(primary) }.padding(8.dp),
                )
                VerticalDivider()
                Text("+", modifier = Modifier.clickable(enabled = !busy) { onAdjustProgress(1) }.padding(12.dp))
            }
            dateItem?.let { item ->
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.track!!.startDate.takeIf { it > 0 }?.let { dateFormat.format(it.toLocalDate()) }
                            ?: stringResource(MR.strings.track_started_reading_date),
                        modifier = Modifier.weight(1f).clickable(enabled = !busy) { onStartDateEdit(item) }.padding(8.dp),
                    )
                    VerticalDivider()
                    Text(
                        text = item.track.finishDate.takeIf { it > 0 }?.let { dateFormat.format(it.toLocalDate()) }
                            ?: stringResource(MR.strings.track_finished_reading_date),
                        modifier = Modifier.weight(1f).clickable(enabled = !busy) { onEndDateEdit(item) }.padding(8.dp),
                    )
                }
            }
            HorizontalDivider()
            IconButton(onClick = { onRemoveTracking(bound) }, enabled = !busy) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(AMR.strings.tracker_remove_selected))
            }
        }
    }
}

@Composable
private fun TrackInfoItem(
    title: String,
    tracker: Tracker,
    // AM -->
    isSeason: Boolean,
    // <-- AM
    status: StringResource?,
    onStatusClick: () -> Unit,
    chapters: String,
    onChaptersClick: () -> Unit,
    score: String?,
    onScoreClick: (() -> Unit)?,
    startDate: String?,
    onStartDateClick: (() -> Unit)?,
    endDate: String?,
    onEndDateClick: (() -> Unit)?,
    onNewSearch: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onRemoved: () -> Unit,
    onCopyLink: () -> Unit,
    preferred: Boolean,
    editMode: Boolean,
    onSetPreferred: () -> Unit,
    selected: Boolean,
    onToggleSelected: () -> Unit,
    skipped: Boolean,
    syncError: Boolean,
    private: Boolean,
    onTogglePrivate: (() -> Unit)?,
    initialFocusModifier: Modifier,
) {
    val context = LocalContext.current
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BadgedBox(
                badge = {
                    if (syncError) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.absoluteOffset(x = (-5).dp),
                        ) {
                            Text("!", modifier = Modifier.padding(horizontal = 3.dp))
                        }
                    } else if (skipped) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.absoluteOffset(x = (-5).dp),
                        ) {
                            Text("–", modifier = Modifier.padding(horizontal = 3.dp))
                        }
                    } else if (selected) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.absoluteOffset(x = (-5).dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = stringResource(AMR.strings.tracker_selected),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    } else if (preferred) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.absoluteOffset(x = (-5).dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = stringResource(AMR.strings.pref_priority_tracker),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    } else if (private) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.absoluteOffset(x = (-5).dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VisibilityOff,
                                contentDescription = stringResource(AMR.strings.tracked_privately),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                },
            ) {
                TrackLogoIcon(
                    tracker = tracker,
                    onClick = if (editMode) onToggleSelected else onOpenInBrowser,
                    onLongClick = if (editMode) onSetPreferred else onCopyLink,
                )
            }
            Box(
                modifier = Modifier
                    .then(initialFocusModifier)
                    .height(48.dp)
                    .weight(1f)
                    .combinedClickable(
                        onClick = onNewSearch,
                        onLongClick = {
                            context.copyToClipboard(title, title)
                        },
                    )
                    .padding(start = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            VerticalDivider()
            TrackInfoItemMenu(
                onOpenInBrowser = onOpenInBrowser,
                onRemoved = onRemoved,
                onCopyLink = onCopyLink,
                private = private,
                onTogglePrivate = onTogglePrivate,
            )
        }

        // AM -->
        if (!isSeason) {
            // <-- AM
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp)),
            ) {
                Column {
                    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                        TrackDetailsItem(
                            modifier = Modifier.weight(1f),
                            text = status?.let { stringResource(it) } ?: "",
                            onClick = onStatusClick,
                        )
                        VerticalDivider()
                        TrackDetailsItem(
                            modifier = Modifier.weight(1f),
                            text = chapters,
                            onClick = onChaptersClick,
                        )
                        if (onScoreClick != null) {
                            VerticalDivider()
                            TrackDetailsItem(
                                modifier = Modifier.weight(1f),
                                text = score,
                                placeholder = stringResource(MR.strings.score),
                                onClick = onScoreClick,
                            )
                        }
                    }

                    if (onStartDateClick != null && onEndDateClick != null) {
                        HorizontalDivider()
                        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                            TrackDetailsItem(
                                modifier = Modifier.weight(1F),
                                text = startDate,
                                placeholder = stringResource(MR.strings.track_started_reading_date),
                                onClick = onStartDateClick,
                            )
                            VerticalDivider()
                            TrackDetailsItem(
                                modifier = Modifier.weight(1F),
                                text = endDate,
                                placeholder = stringResource(MR.strings.track_finished_reading_date),
                                onClick = onEndDateClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val UNSET_TEXT_ALPHA = 0.5F

@Composable
private fun TrackDetailsItem(
    text: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxHeight()
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text ?: placeholder,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (text == null) UNSET_TEXT_ALPHA else 1f),
        )
    }
}

@Composable
private fun TrackInfoItemEmpty(
    tracker: Tracker,
    onNewSearch: () -> Unit,
    initialFocusModifier: Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackLogoIcon(tracker)
        TextButton(
            onClick = onNewSearch,
            modifier = Modifier
                .then(initialFocusModifier)
                .padding(start = 16.dp)
                .weight(1f),
        ) {
            Text(text = stringResource(MR.strings.add_tracking))
        }
    }
}

@Composable
private fun TrackInfoItemMenu(
    onOpenInBrowser: () -> Unit,
    onRemoved: () -> Unit,
    onCopyLink: () -> Unit,
    private: Boolean,
    onTogglePrivate: (() -> Unit)?,
) {
    val isTvUi = isTvUi()
    val menuFocusRequester = remember { FocusRequester() }
    val firstMenuItemFocusRequester = remember { FocusRequester() }
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(expanded, isTvUi) {
        if (expanded && isTvUi) firstMenuItemFocusRequester.requestFocus()
    }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(
            modifier = Modifier.focusRequester(menuFocusRequester),
            onClick = { expanded = true },
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(MR.strings.label_more),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                if (isTvUi) menuFocusRequester.requestFocus()
            },
        ) {
            DropdownMenuItem(
                modifier = Modifier.focusRequester(firstMenuItemFocusRequester),
                text = { Text(stringResource(MR.strings.action_open_in_browser)) },
                onClick = {
                    onOpenInBrowser()
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.action_copy_link)) },
                onClick = {
                    onCopyLink()
                    expanded = false
                },
            )
            if (onTogglePrivate != null) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (private) {
                                    AMR.strings.action_toggle_private_off
                                } else {
                                    AMR.strings.action_toggle_private_on
                                },
                            ),
                        )
                    },
                    onClick = {
                        onTogglePrivate()
                        expanded = false
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.action_remove)) },
                onClick = {
                    onRemoved()
                    expanded = false
                },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TrackInfoDialogHomePreviews(
    @PreviewParameter(TrackInfoDialogHomePreviewProvider::class)
    content: @Composable () -> Unit,
) {
    TachiyomiPreviewTheme { content() }
}
