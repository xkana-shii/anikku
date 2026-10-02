package eu.kanade.tachiyomi.ui.manga.track

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.manga.model.toSManga
import eu.kanade.domain.track.interactor.RefreshResult
import eu.kanade.domain.track.interactor.RefreshTracks
import eu.kanade.domain.track.interactor.UpdateTracks
import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.track.TrackChapterSelector
import eu.kanade.presentation.track.TrackDateSelector
import eu.kanade.presentation.track.TrackInfoDialogHome
import eu.kanade.presentation.track.TrackScoreSelector
import eu.kanade.presentation.track.TrackStatusSelector
import eu.kanade.presentation.track.TrackerSearch
import eu.kanade.presentation.track.components.TrackLogoIcon
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.animesource.TrackerIdMetadataSource
import eu.kanade.tachiyomi.animesource.model.FetchType
import eu.kanade.tachiyomi.data.track.DeletableTracker
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.source.online.all.MergedSource
import eu.kanade.tachiyomi.util.lang.convertEpochMillisZone
import eu.kanade.tachiyomi.util.lang.toLocalDate
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toast
import exh.source.MERGED_SOURCE_ID
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.QuerySanitizer.sanitize
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.GetMergedReferencesById
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.DeleteTrack
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.model.Track
import tachiyomi.i18n.MR
import tachiyomi.i18n.ank.AMR
import tachiyomi.presentation.core.components.LabeledCheckbox
import tachiyomi.presentation.core.components.material.AlertDialogContent
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

data class TrackInfoDialogHomeScreen(
    private val mangaId: Long,
    private val mangaTitle: String,
    // AM -->
    private val isSeason: Boolean,
    // <-- AM
    private val sourceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val screenModel = rememberScreenModel {
            Model(
                mangaId,
                sourceId,
                // AM -->
                isSeason,
                // <-- AM
            )
        }

        val dateFormat = remember { UiPreferences.dateFormat(Injekt.get<UiPreferences>().dateFormat().get()) }
        val state by screenModel.state.collectAsState()
        val bound = state.trackItems.filter { it.track != null }
        var editor by remember { mutableStateOf<Pair<TrackItem, UnifiedTrackField>?>(null) }
        editor?.let { (item, field) ->
            UnifiedTrackEditor(
                item = item,
                field = field,
                onApply = {
                    screenModel.updateUnified(it)
                    editor = null
                },
                onDismiss = { editor = null },
            )
            return
        }

        TrackInfoDialogHome(
            trackItems = state.trackItems,
            seriesTitle = mangaTitle,
            dateFormat = dateFormat,
            // AM -->
            isSeason = isSeason,
            isActive = navigator.lastItem == this,
            // <-- AM
            onStatusClick = {
                if (bound.size >= 2) {
                    editor = it to UnifiedTrackField.STATUS
                } else {
                    navigator.push(
                        TrackStatusSelectorScreen(
                            track = it.track!!,
                            serviceId = it.tracker.id,
                        ),
                    )
                }
            },
            onChapterClick = {
                if (bound.size >= 2) {
                    editor = it to UnifiedTrackField.PROGRESS
                } else {
                    navigator.push(
                        TrackChapterSelectorScreen(
                            track = it.track!!,
                            serviceId = it.tracker.id,
                        ),
                    )
                }
            },
            onScoreClick = {
                if (bound.size >= 2) {
                    editor = it to UnifiedTrackField.SCORE
                } else {
                    navigator.push(
                        TrackScoreSelectorScreen(
                            track = it.track!!,
                            serviceId = it.tracker.id,
                        ),
                    )
                }
            },
            onStartDateEdit = {
                if (bound.size >= 2) {
                    editor = it to UnifiedTrackField.START_DATE
                } else {
                    navigator.push(
                        TrackDateSelectorScreen(
                            track = it.track!!,
                            serviceId = it.tracker.id,
                            start = true,
                        ),
                    )
                }
            },
            onEndDateEdit = {
                if (bound.size >= 2) {
                    editor = it to UnifiedTrackField.END_DATE
                } else {
                    navigator.push(
                        TrackDateSelectorScreen(
                            track = it.track!!,
                            serviceId = it.tracker.id,
                            start = false,
                        ),
                    )
                }
            },
            onNewSearch = {
                if (it.tracker is EnhancedTracker) {
                    screenModel.registerEnhancedTracking(it)
                } else {
                    screenModel.newSearch(navigator, it, mangaTitle)
                }
            },
            onOpenInBrowser = { openTrackerInBrowser(context, it) },
            onRemoved = {
                navigator.push(
                    TrackerRemoveScreen(
                        mangaId = mangaId,
                        track = it.track!!,
                        serviceId = it.tracker.id,
                    ),
                )
            },
            onCopyLink = { context.copyTrackerLink(it) },
            onTogglePrivate = screenModel::togglePrivate,
            preferredId = state.preferredId,
            editMode = state.editMode,
            onToggleEditMode = screenModel::toggleEditMode,
            onSetPreferredTracker = screenModel::setPreferredTracker,
            selectedTrackerIds = state.selectedTrackerIds,
            onToggleTrackerSelection = screenModel::toggleTrackerSelection,
            onAdjustProgress = { delta ->
                val current = bound.firstOrNull { it.tracker.id == state.preferredId } ?: bound.firstOrNull()
                current?.track?.let { screenModel.updateUnified(UpdateTracks.Change.Progress((it.lastChapterRead.toInt() + delta).coerceAtLeast(0))) }
            },
            onRemoveTracking = { selected ->
                navigator.push(TrackerBatchRemoveScreen(mangaId, selected.map { it.tracker.id }.toSet()))
            },
            skippedTrackerIds = state.skippedTrackerIds,
            errorTrackerIds = state.errorTrackerIds,
            busy = state.busy,
        )
    }

    /**
     * Opens registered tracker url in browser
     */
    private fun openTrackerInBrowser(context: Context, trackItem: TrackItem) {
        val url = trackItem.track?.remoteUrl ?: return
        if (url.isNotBlank()) {
            context.openInBrowser(url)
        }
    }

    private fun Context.copyTrackerLink(trackItem: TrackItem) {
        val url = trackItem.track?.remoteUrl ?: return
        if (url.isNotBlank()) {
            copyToClipboard(url, url)
        }
    }

    private class Model(
        private val mangaId: Long,
        private val sourceId: Long,
        // AM -->
        private val isSeason: Boolean,
        // <-- AM
        private val getTracks: GetTracks = Injekt.get(),
        // SY -->
        private val trackerManager: TrackerManager = Injekt.get(),
        // SY <--
        // KMK -->
        private val sourceManager: SourceManager = Injekt.get(),
        private val trackPreferences: TrackPreferences = Injekt.get(),
        // KMK <--
    ) : StateScreenModel<Model.State>(State()) {
        // KMK -->
        private val getMangaById: GetManga by injectLazy()
        private val getMergedReferencesById: GetMergedReferencesById by injectLazy()
        // KMK <--

        init {
            screenModelScope.launch {
                // AM -->
                if (!isSeason) {
                    // <-- AM
                    refreshTrackers()
                }
            }

            screenModelScope.launch {
                getTracks.subscribe(mangaId)
                    .catch { logcat(LogPriority.ERROR, it) }
                    .distinctUntilChanged()
                    .map { it.mapToTrackItem() }
                    .collectLatest { trackItems ->
                        val applicable = trackItems.filter { it.track != null }.map { it.tracker.id }.toSet()
                        mutableState.update {
                            it.copy(
                                trackItems = trackItems,
                                preferredId = trackPreferences.resolvePreferredTracker(mangaId, applicable),
                                selectedTrackerIds = it.selectedTrackerIds intersect applicable,
                                editMode = if (applicable.size >= 2) it.editMode else false,
                            )
                        }
                    }
            }
        }

        // KMK -->
        private suspend fun getMangaForTracking(item: TrackItem): Manga? {
            if (sourceId != MERGED_SOURCE_ID) {
                return getMangaById.await(mangaId)
            }
            item.tracker as EnhancedTracker
            val references = getMergedReferencesById.await(mangaId)
            return references.distinctBy { it.mangaSourceId }.firstNotNullOfOrNull { ref ->
                sourceManager.get(ref.mangaSourceId)
                    ?.takeIf(item.tracker::accept)
                    ?.let { ref.mangaId?.let { mangaId -> getMangaById.await(mangaId) } }
            }
        }
        // KMK <--

        fun registerEnhancedTracking(item: TrackItem) {
            item.tracker as EnhancedTracker
            screenModelScope.launchNonCancellable {
                val anime = getMangaForTracking(item) ?: return@launchNonCancellable
                try {
                    // AM -->
                    val matchResult = when (anime.fetchType) {
                        FetchType.Episodes -> item.tracker.match(anime) ?: throw Exception()
                        FetchType.Seasons -> item.tracker.matchSeason(anime) ?: throw Exception()
                    }
                    item.tracker.register(matchResult, anime)
                    // <-- AM
                } catch (_: Exception) {
                    withUIContext { Injekt.get<Application>().toast(MR.strings.error_no_match) }
                }
            }
        }

        fun newSearch(navigator: Navigator, item: TrackItem, animeTitle: String) {
            screenModelScope.launchNonCancellable {
                if (item.track == null && registerUsingSourceMetadata(item)) return@launchNonCancellable
                navigator.push(
                    TrackerSearchScreen(
                        mangaId = mangaId,
                        initialQuery = item.track?.title ?: animeTitle,
                        currentUrl = item.track?.remoteUrl,
                        serviceId = item.tracker.id,
                    ),
                )
            }
        }

        private suspend fun registerUsingSourceMetadata(item: TrackItem): Boolean {
            val source = sourceManager.get(sourceId) as? TrackerIdMetadataSource ?: return false
            val anime = getMangaById.await(mangaId) ?: return false
            return try {
                val metadata = source.getTrackerIdMetadata(anime.toSManga()) ?: return false
                val remoteId = when (item.tracker.id) {
                    TrackerManager.ANILIST -> metadata.aniListId
                    TrackerManager.MYANIMELIST -> metadata.myAnimeListId
                    TrackerManager.KITSU -> metadata.kitsuId
                    else -> null
                }?.takeIf { it.isNotBlank() } ?: return false
                val exact = item.tracker.searchById(remoteId) ?: return false
                item.tracker.register(exact, anime)
                true
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed exact tracker binding from source metadata" }
                false
            }
        }

        private suspend fun refreshTrackers() {
            val refreshTracks = Injekt.get<RefreshTracks>()
            val context = Injekt.get<Application>()

            refreshTracks.await(mangaId)
                // AM -->
                .filterIsInstance<RefreshResult.Failure>()
                // <-- AM
                .forEach { (track, e) ->
                    logcat(LogPriority.ERROR, e) {
                        "Failed to refresh track data animeId=$mangaId for service ${track.id}"
                    }
                    withUIContext {
                        context.toast(
                            context.stringResource(
                                MR.strings.track_error,
                                track.name,
                                e.message ?: "",
                            ),
                        )
                    }
                }
        }

        fun togglePrivate(item: TrackItem) {
            screenModelScope.launchNonCancellable {
                item.tracker.setRemotePrivate(item.track!!.toDbTrack(), !item.track.private)
            }
        }

        fun toggleEditMode() {
            mutableState.update {
                it.copy(
                    editMode = !it.editMode,
                    selectedTrackerIds = if (it.editMode) emptySet() else it.selectedTrackerIds,
                )
            }
        }

        fun toggleTrackerSelection(item: TrackItem) {
            mutableState.update {
                val selected = it.selectedTrackerIds.toMutableSet()
                if (!selected.add(item.tracker.id)) selected.remove(item.tracker.id)
                it.copy(selectedTrackerIds = selected)
            }
        }

        fun setPreferredTracker(item: TrackItem) {
            val next = item.tracker.id.takeUnless { it == state.value.preferredId }
            trackPreferences.setPreferredTrackerForAnime(mangaId, next)
            val applicable = state.value.trackItems.filter { it.track != null }.map { it.tracker.id }.toSet()
            mutableState.update { it.copy(preferredId = trackPreferences.resolvePreferredTracker(mangaId, applicable)) }
        }

        fun updateUnified(change: UpdateTracks.Change) {
            if (state.value.busy) return
            screenModelScope.launch {
                mutableState.update { it.copy(busy = true, skippedTrackerIds = emptySet(), errorTrackerIds = emptySet()) }
                try {
                    val result = withIOContext { Injekt.get<UpdateTracks>().awaitDetailed(mangaId, change) }
                    result.failures.forEach { (tracker, error) ->
                        logcat(LogPriority.ERROR, error) {
                            "Unified tracker update failed for ${tracker?.name.orEmpty()}"
                        }
                    }
                    mutableState.update {
                        it.copy(
                            skippedTrackerIds = result.skippedTrackerIds,
                            errorTrackerIds = result.failedTrackerIds,
                        )
                    }
                    if (result.failures.isNotEmpty()) {
                        withUIContext { Injekt.get<Application>().toast(AMR.strings.tracker_update_partial_failure) }
                    }
                } finally {
                    mutableState.update { it.copy(busy = false) }
                }
            }
        }

        private suspend fun List<Track>.mapToTrackItem(): List<TrackItem> {
            val loggedInTrackers = trackerManager.loggedInTrackers()
            val source = sourceManager.getOrStub(sourceId)
            return loggedInTrackers
                // Map to TrackItem
                .map { service -> TrackItem(find { it.trackerId == service.id }, service) }
                // Show only if the service supports this manga's source
                // KMK -->
                .let { trackers ->
                    val sources = if (source is MergedSource) {
                        sourceManager.getMergedSources(mangaId)
                    } else {
                        listOf(source)
                    }
                    trackers.filter { (it.tracker as? EnhancedTracker)?.accept(sources) ?: true }
                }
                // KMK <--
                // AM -->
                // Only show enhanced trackers for seasons for now
                .filter { !isSeason || it.tracker is EnhancedTracker }
            // <-- AM
        }

        @Immutable
        data class State(
            val trackItems: List<TrackItem> = emptyList(),
            val preferredId: Long? = null,
            val editMode: Boolean = false,
            val selectedTrackerIds: Set<Long> = emptySet(),
            val skippedTrackerIds: Set<Long> = emptySet(),
            val errorTrackerIds: Set<Long> = emptySet(),
            val busy: Boolean = false,
        )
    }
}

private data class TrackStatusSelectorScreen(
    private val track: Track,
    private val serviceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel {
            Model(
                track = track,
                tracker = Injekt.get<TrackerManager>().get(serviceId)!!,
            )
        }
        val state by screenModel.state.collectAsState()
        TrackStatusSelector(
            selection = state.selection,
            onSelectionChange = screenModel::setSelection,
            selections = remember { screenModel.getSelections() },
            onConfirm = {
                screenModel.setStatus()
                navigator.pop()
            },
            onDismissRequest = navigator::pop,
        )
    }

    private class Model(
        private val track: Track,
        private val tracker: Tracker,
    ) : StateScreenModel<Model.State>(State(track.status)) {

        fun getSelections(): Map<Long, StringResource?> {
            return tracker.getStatusList().associateWith { tracker.getStatus(it) }
        }

        fun setSelection(selection: Long) {
            mutableState.update { it.copy(selection = selection) }
        }

        fun setStatus() {
            screenModelScope.launchNonCancellable {
                tracker.setRemoteStatus(track.toDbTrack(), state.value.selection)
            }
        }

        @Immutable
        data class State(
            val selection: Long,
        )
    }
}

private data class TrackChapterSelectorScreen(
    private val track: Track,
    private val serviceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel {
            Model(
                track = track,
                tracker = Injekt.get<TrackerManager>().get(serviceId)!!,
            )
        }
        val state by screenModel.state.collectAsState()

        TrackChapterSelector(
            selection = state.selection,
            onSelectionChange = screenModel::setSelection,
            range = remember { screenModel.getRange() },
            onConfirm = {
                screenModel.setChapter()
                navigator.pop()
            },
            onDismissRequest = navigator::pop,
        )
    }

    private class Model(
        private val track: Track,
        private val tracker: Tracker,
    ) : StateScreenModel<Model.State>(State(track.lastChapterRead.toInt())) {

        fun getRange(): Iterable<Int> {
            val endRange = if (track.totalChapters > 0) {
                track.totalChapters
            } else {
                10000
            }
            return 0..endRange.toInt()
        }

        fun setSelection(selection: Int) {
            mutableState.update { it.copy(selection = selection) }
        }

        fun setChapter() {
            screenModelScope.launchNonCancellable {
                tracker.setRemoteLastChapterRead(track.toDbTrack(), state.value.selection)
            }
        }

        @Immutable
        data class State(
            val selection: Int,
        )
    }
}

private data class TrackScoreSelectorScreen(
    private val track: Track,
    private val serviceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel {
            Model(
                track = track,
                tracker = Injekt.get<TrackerManager>().get(serviceId)!!,
            )
        }
        val state by screenModel.state.collectAsState()

        TrackScoreSelector(
            selection = state.selection,
            onSelectionChange = screenModel::setSelection,
            selections = remember { screenModel.getSelections() },
            onConfirm = {
                screenModel.setScore()
                navigator.pop()
            },
            onDismissRequest = navigator::pop,
        )
    }

    private class Model(
        private val track: Track,
        private val tracker: Tracker,
    ) : StateScreenModel<Model.State>(State(tracker.displayScore(track))) {

        fun getSelections(): ImmutableList<String> {
            return tracker.getScoreList()
        }

        fun setSelection(selection: String) {
            mutableState.update { it.copy(selection = selection) }
        }

        fun setScore() {
            screenModelScope.launchNonCancellable {
                tracker.setRemoteScore(track.toDbTrack(), state.value.selection)
            }
        }

        @Immutable
        data class State(
            val selection: String,
        )
    }
}

private data class TrackDateSelectorScreen(
    private val track: Track,
    private val serviceId: Long,
    private val start: Boolean,
) : Screen() {

    @Transient
    private val selectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            val targetDate = Instant.ofEpochMilli(utcTimeMillis).toLocalDate(ZoneOffset.UTC)

            // Disallow future dates
            if (targetDate > LocalDate.now(ZoneOffset.UTC)) return false

            return when {
                // Disallow setting start date after finish date
                start && track.finishDate > 0 -> {
                    val finishDate = Instant.ofEpochMilli(track.finishDate).toLocalDate(ZoneOffset.UTC)
                    targetDate <= finishDate
                }
                // Disallow setting finish date before start date
                !start && track.startDate > 0 -> {
                    val startDate = Instant.ofEpochMilli(track.startDate).toLocalDate(ZoneOffset.UTC)
                    startDate <= targetDate
                }
                else -> {
                    true
                }
            }
        }

        override fun isSelectableYear(year: Int): Boolean {
            // Disallow future years
            if (year > LocalDate.now(ZoneOffset.UTC).year) return false

            return when {
                // Disallow setting start year after finish year
                start && track.finishDate > 0 -> {
                    val finishDate = Instant.ofEpochMilli(track.finishDate).toLocalDate(ZoneOffset.UTC)
                    year <= finishDate.year
                }
                // Disallow setting finish year before start year
                !start && track.startDate > 0 -> {
                    val startDate = Instant.ofEpochMilli(track.startDate).toLocalDate(ZoneOffset.UTC)
                    startDate.year <= year
                }
                else -> {
                    true
                }
            }
        }
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel {
            Model(
                track = track,
                tracker = Injekt.get<TrackerManager>().get(serviceId)!!,
                start = start,
            )
        }

        val canRemove = if (start) {
            track.startDate > 0
        } else {
            track.finishDate > 0
        }
        TrackDateSelector(
            title = if (start) {
                stringResource(MR.strings.track_started_reading_date)
            } else {
                stringResource(MR.strings.track_finished_reading_date)
            },
            initialSelectedDateMillis = screenModel.initialSelection,
            selectableDates = selectableDates,
            onConfirm = {
                screenModel.setDate(it)
                navigator.pop()
            },
            onRemove = { screenModel.confirmRemoveDate(navigator) }.takeIf { canRemove },
            onDismissRequest = navigator::pop,
        )
    }

    private class Model(
        private val track: Track,
        private val tracker: Tracker,
        private val start: Boolean,
    ) : ScreenModel {

        // In UTC
        val initialSelection: Long
            get() {
                val millis = (if (start) track.startDate else track.finishDate)
                    .takeIf { it != 0L }
                    ?: Instant.now().toEpochMilli()
                return millis.convertEpochMillisZone(ZoneOffset.systemDefault(), ZoneOffset.UTC)
            }

        // In UTC
        fun setDate(millis: Long) {
            // Convert to local time
            val localMillis = millis.convertEpochMillisZone(ZoneOffset.UTC, ZoneOffset.systemDefault())
            screenModelScope.launchNonCancellable {
                if (start) {
                    tracker.setRemoteStartDate(track.toDbTrack(), localMillis)
                } else {
                    tracker.setRemoteFinishDate(track.toDbTrack(), localMillis)
                }
            }
        }

        fun confirmRemoveDate(navigator: Navigator) {
            navigator.push(TrackDateRemoverScreen(track, tracker.id, start))
        }
    }
}

private data class TrackDateRemoverScreen(
    private val track: Track,
    private val serviceId: Long,
    private val start: Boolean,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel {
            Model(
                track = track,
                tracker = Injekt.get<TrackerManager>().get(serviceId)!!,
                start = start,
            )
        }
        AlertDialogContent(
            modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                )
            },
            title = {
                Text(
                    text = stringResource(MR.strings.track_remove_date_conf_title),
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                val serviceName = screenModel.getServiceName()
                Text(
                    text = if (start) {
                        stringResource(MR.strings.track_remove_start_date_conf_text, serviceName)
                    } else {
                        stringResource(MR.strings.track_remove_finish_date_conf_text, serviceName)
                    },
                )
            },
            buttons = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small, Alignment.End),
                ) {
                    TextButton(onClick = navigator::pop) {
                        Text(text = stringResource(MR.strings.action_cancel))
                    }
                    FilledTonalButton(
                        onClick = {
                            screenModel.removeDate()
                            navigator.popUntil { it is TrackInfoDialogHomeScreen }
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Text(text = stringResource(MR.strings.action_remove))
                    }
                }
            },
        )
    }

    private class Model(
        private val track: Track,
        private val tracker: Tracker,
        private val start: Boolean,
    ) : ScreenModel {

        fun getServiceName() = tracker.name

        fun removeDate() {
            screenModelScope.launchNonCancellable {
                if (start) {
                    tracker.setRemoteStartDate(track.toDbTrack(), 0)
                } else {
                    tracker.setRemoteFinishDate(track.toDbTrack(), 0)
                }
            }
        }
    }
}

data class TrackerSearchScreen(
    private val mangaId: Long,
    private val initialQuery: String,
    private val currentUrl: String?,
    private val serviceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel {
            Model(
                mangaId = mangaId,
                currentUrl = currentUrl,
                initialQuery = initialQuery,
                tracker = Injekt.get<TrackerManager>().get(serviceId)!!,
            )
        }

        val state by screenModel.state.collectAsState()

        val textFieldState = rememberTextFieldState(initialQuery)
        TrackerSearch(
            state = textFieldState,
            onDispatchQuery = { screenModel.trackingSearch(textFieldState.text.toString()) },
            queryResult = state.queryResult,
            selected = state.selected,
            onSelectedChange = screenModel::updateSelection,
            onConfirmSelection = f@{ private: Boolean ->
                val selected = state.selected ?: return@f
                selected.private = private
                screenModel.registerTracking(selected)
                navigator.pop()
            },
            onDismissRequest = navigator::pop,
            supportsPrivateTracking = screenModel.supportsPrivateTracking,
        )
    }

    private class Model(
        private val mangaId: Long,
        private val currentUrl: String? = null,
        initialQuery: String,
        private val tracker: Tracker,
    ) : StateScreenModel<Model.State>(State()) {

        val supportsPrivateTracking = tracker.supportsPrivateTracking

        init {
            // Run search on first launch
            if (initialQuery.isNotBlank()) {
                trackingSearch(initialQuery)
            }
        }

        fun trackingSearch(query: String) {
            screenModelScope.launch {
                // To show loading state
                mutableState.update { it.copy(queryResult = null, selected = null) }

                val result = withIOContext {
                    try {
                        val results = tracker.search(query.sanitize())
                        Result.success(results)
                    } catch (e: Throwable) {
                        Result.failure(e)
                    }
                }
                mutableState.update { oldState ->
                    oldState.copy(
                        queryResult = result,
                        selected = result.getOrNull()?.find { it.tracking_url == currentUrl },
                    )
                }
            }
        }

        fun registerTracking(item: TrackSearch) {
            screenModelScope.launchNonCancellable {
                // AM -->
                val anime = Injekt.get<GetManga>().await(mangaId) ?: return@launchNonCancellable
                tracker.register(item, anime)
                // <-- AM
            }
        }

        fun updateSelection(selected: TrackSearch) {
            mutableState.update { it.copy(selected = selected) }
        }

        @Immutable
        data class State(
            val queryResult: Result<List<TrackSearch>>? = null,
            val selected: TrackSearch? = null,
        )
    }
}

private data class TrackerBatchRemoveScreen(
    private val animeId: Long,
    private val trackerIds: Set<Long>,
) : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val screenModel = rememberScreenModel { Model(animeId) }
        var removeRemote by remember { mutableStateOf(false) }
        var selectedIds by remember { mutableStateOf(trackerIds) }
        val trackers = remember(trackerIds) {
            val manager = Injekt.get<TrackerManager>()
            trackerIds.mapNotNull(manager::get)
        }
        AlertDialogContent(
            modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = {
                Text(
                    text = stringResource(AMR.strings.tracker_remove_selected),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Column {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
                        trackers.forEach { tracker ->
                            BadgedBox(
                                badge = {
                                    if (tracker.id in selectedIds) {
                                        Badge {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = stringResource(AMR.strings.tracker_selected),
                                                modifier = Modifier.size(12.dp),
                                            )
                                        }
                                    }
                                },
                            ) {
                                TrackLogoIcon(
                                    tracker = tracker,
                                    onClick = {
                                        selectedIds = if (tracker.id in selectedIds) selectedIds - tracker.id else selectedIds + tracker.id
                                    },
                                )
                            }
                        }
                    }
                    LabeledCheckbox(
                        label = stringResource(AMR.strings.tracker_remove_remote_selected),
                        checked = removeRemote,
                        onCheckedChange = { removeRemote = it },
                    )
                }
            },
            buttons = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small, Alignment.End),
                ) {
                    TextButton(onClick = navigator::pop) { Text(stringResource(MR.strings.action_cancel)) }
                    FilledTonalButton(
                        onClick = {
                            screenModel.remove(context, selectedIds, removeRemote)
                            navigator.pop()
                        },
                        enabled = selectedIds.isNotEmpty(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Text(stringResource(MR.strings.action_remove))
                    }
                }
            },
        )
    }

    private class Model(
        private val animeId: Long,
    ) : ScreenModel {
        fun remove(context: Context, selectedIds: Set<Long>, remotely: Boolean) {
            screenModelScope.launchNonCancellable {
                val failures = Injekt.get<UpdateTracks>().remove(animeId, selectedIds, remotely)
                if (failures.isNotEmpty()) {
                    withUIContext { context.toast(AMR.strings.tracker_remove_partial_failure) }
                }
            }
        }
    }
}

private data class TrackerRemoveScreen(
    private val mangaId: Long,
    private val track: Track,
    private val serviceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel {
            Model(
                mangaId = mangaId,
                track = track,
                tracker = Injekt.get<TrackerManager>().get(serviceId)!!,
            )
        }
        val serviceName = screenModel.getName()
        var removeRemoteTrack by remember { mutableStateOf(false) }
        AlertDialogContent(
            modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                )
            },
            title = {
                Text(
                    text = stringResource(MR.strings.track_delete_title, serviceName),
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    Text(
                        text = stringResource(MR.strings.track_delete_text, serviceName),
                    )

                    if (screenModel.isDeletable()) {
                        LabeledCheckbox(
                            label = stringResource(MR.strings.track_delete_remote_text, serviceName),
                            checked = removeRemoteTrack,
                            onCheckedChange = { removeRemoteTrack = it },
                        )
                    }
                }
            },
            buttons = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        MaterialTheme.padding.small,
                        Alignment.End,
                    ),
                ) {
                    TextButton(onClick = navigator::pop) {
                        Text(text = stringResource(MR.strings.action_cancel))
                    }
                    FilledTonalButton(
                        onClick = {
                            screenModel.unregisterTracking(serviceId)
                            if (removeRemoteTrack) screenModel.deleteMangaFromService()
                            navigator.pop()
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Text(text = stringResource(MR.strings.action_ok))
                    }
                }
            },
        )
    }

    private class Model(
        private val mangaId: Long,
        private val track: Track,
        private val tracker: Tracker,
        private val deleteTrack: DeleteTrack = Injekt.get(),
    ) : ScreenModel {

        fun getName() = tracker.name

        fun isDeletable() = tracker is DeletableTracker

        fun deleteMangaFromService() {
            screenModelScope.launchNonCancellable {
                try {
                    (tracker as DeletableTracker).delete(track)
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e) { "Failed to delete anime entry from service" }
                }
            }
        }

        fun unregisterTracking(serviceId: Long) {
            screenModelScope.launchNonCancellable { deleteTrack.await(mangaId, serviceId) }
        }
    }
}
