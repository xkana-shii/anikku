package eu.kanade.presentation.library.tracker

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import eu.kanade.tachiyomi.data.track.simkl.Simkl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.track.interactor.GetTracks
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TrackerMangaListScreenModel(
    private val getLibraryManga: GetLibraryManga = Injekt.get(),
    private val getTracks: GetTracks = Injekt.get(),
    private val trackerManager: TrackerManager = Injekt.get(),
) : StateScreenModel<TrackerMangaListState>(TrackerMangaListState()) {
    val trackers: List<Tracker> = trackerManager.loggedInTrackers()
        .filterNot { it is EnhancedTracker }
        .filter { it is Anilist || it is MyAnimeList || it is Simkl }
    private var tracker: Tracker? = trackers.firstOrNull()
    private var libraryMangaIds: Set<Long> = emptySet()
    private var libraryTracks: List<tachiyomi.domain.track.model.Track> = emptyList()
    private var libraryRemoteIds: Set<Long> = emptySet()
    private var trackerGeneration = 0

    init {
        screenModelScope.launchIO {
            getLibraryManga.subscribe().collect { library ->
                libraryMangaIds = library.map { it.id }.toSet()
                libraryTracks = getTracks.await()
                updateLibraryRemoteIds()
                mutableState.update {
                    it.copy(
                        trackerId = tracker?.id,
                        statusList = tracker?.trackerListStatuses().orEmpty(),
                        getStatusRes = tracker?.let { service -> service::getStatus } ?: { null },
                    )
                }
            }
        }
    }

    fun loadNextPage(tabIndex: Int) {
        val service = tracker ?: return
        val generation = trackerGeneration
        val current = state.value.tabs[tabIndex] ?: TabMangaList()
        if (current.isLoading || current.endReached) return
        val status = state.value.statusList.getOrNull(tabIndex) ?: return
        mutableState.update { it.copy(tabs = it.tabs + (tabIndex to current.copy(isLoading = true))) }
        screenModelScope.launchIO {
            try {
                val result = service.getPaginatedMangaList(current.page, status)
                if (generation != trackerGeneration) return@launchIO
                val items = result.filterNot { it.remoteId in libraryRemoteIds }
                mutableState.update { state ->
                    state.copy(
                        tabs = state.tabs + (
                            tabIndex to current.copy(
                                items = current.items + items,
                                page = current.page + 1,
                                isLoading = false,
                                endReached = result.isEmpty(),
                                loadError = false,
                            )
                            ),
                    )
                }
                if (items.isEmpty() && result.isNotEmpty()) loadNextPage(tabIndex)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (generation == trackerGeneration) {
                    mutableState.update { state ->
                        state.copy(tabs = state.tabs + (tabIndex to current.copy(isLoading = false, loadError = true)))
                    }
                }
            }
        }
    }

    fun changeTab(index: Int) {
        mutableState.update { it.copy(currentTabIndex = index) }
        loadNextPage(index)
    }

    fun changeTracker(id: Long) {
        trackerGeneration++
        tracker = trackers.firstOrNull { it.id == id }
        updateLibraryRemoteIds()
        mutableState.update {
            TrackerMangaListState(
                trackerId = tracker?.id,
                statusList = tracker?.trackerListStatuses().orEmpty(),
                getStatusRes = tracker?.let { service -> service::getStatus } ?: { null },
                trackerSelectDialog = false,
            )
        }
    }

    fun toggleTrackerSelectDialog() {
        mutableState.update { it.copy(trackerSelectDialog = !it.trackerSelectDialog) }
    }

    fun getTrackerName() = tracker?.name.orEmpty()

    private fun updateLibraryRemoteIds() {
        libraryRemoteIds = libraryTracks
            .filter { it.mangaId in libraryMangaIds && it.trackerId == tracker?.id }
            .map { it.remoteId }
            .toSet()
    }

    private fun Tracker.trackerListStatuses(): List<Long> {
        return getStatusList().filterNot { this is Simkl && it == Simkl.REWATCHING }
    }
}

@Immutable
data class TrackerMangaListState(
    val trackerId: Long? = null,
    val statusList: List<Long> = emptyList(),
    val getStatusRes: (Long) -> StringResource? = { null },
    val tabs: Map<Int, TabMangaList> = emptyMap(),
    val currentTabIndex: Int = 0,
    val trackerSelectDialog: Boolean = false,
)

@Immutable
data class TabMangaList(
    val items: List<TrackMangaMetadata> = emptyList(),
    val page: Int = 1,
    val endReached: Boolean = false,
    val isLoading: Boolean = false,
    val loadError: Boolean = false,
)
