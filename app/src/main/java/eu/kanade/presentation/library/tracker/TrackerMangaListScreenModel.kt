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
    private var libraryRemoteIds: Set<Long> = emptySet()

    init {
        screenModelScope.launchIO {
            getLibraryManga.subscribe().collect { library ->
                val ids = library.map { it.id }.toSet()
                libraryRemoteIds = getTracks.await()
                    .filter { it.mangaId in ids && it.trackerId == tracker?.id }
                    .map { it.remoteId }
                    .toSet()
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
        val current = state.value.tabs[tabIndex] ?: TabMangaList()
        if (current.isLoading || current.endReached) return
        val status = state.value.statusList.getOrNull(tabIndex) ?: return
        mutableState.update { it.copy(tabs = it.tabs + (tabIndex to current.copy(isLoading = true))) }
        screenModelScope.launchIO {
            runCatching { service.getPaginatedMangaList(current.page, status) }
                .onSuccess { result ->
                    val items = result.filterNot { it.remoteId in libraryRemoteIds }
                    mutableState.update { state ->
                        state.copy(
                            tabs = state.tabs + (
                                tabIndex to current.copy(
                                    items = current.items + items,
                                    page = current.page + 1,
                                    isLoading = false,
                                    endReached = items.isEmpty(),
                                )
                                ),
                        )
                    }
                }
                .onFailure {
                    mutableState.update { state -> state.copy(tabs = state.tabs + (tabIndex to current.copy(isLoading = false))) }
                }
        }
    }

    fun changeTab(index: Int) {
        mutableState.update { it.copy(currentTabIndex = index) }
        loadNextPage(index)
    }

    fun changeTracker(id: Long) {
        tracker = trackers.firstOrNull { it.id == id }
        mutableState.update {
            TrackerMangaListState(
                trackerId = tracker?.id,
                statusList = tracker?.trackerListStatuses().orEmpty(),
                getStatusRes = tracker?.let { service -> service::getStatus } ?: { null },
            )
        }
    }

    fun getTrackerName() = tracker?.name.orEmpty()

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
)

@Immutable
data class TabMangaList(
    val items: List<TrackMangaMetadata> = emptyList(),
    val page: Int = 1,
    val endReached: Boolean = false,
    val isLoading: Boolean = false,
)
