package eu.kanade.presentation.library.tracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.library.tracker.components.TrackStatusTabs
import eu.kanade.presentation.library.tracker.components.animeListItem
import eu.kanade.presentation.track.components.TrackLogoIcon
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

class TrackerMangaListScreen : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel { TrackerMangaListScreenModel() }
        val state by model.state.collectAsState()
        val scope = rememberCoroutineScope()
        val scrollStates = remember { mutableStateMapOf<Int, Pair<Int, Int>>() }

        Scaffold(
            topBar = { scrollBehavior ->
                TrackerMangaListAppBar(
                    title = model.getTrackerName(),
                    scrollBehavior = scrollBehavior,
                    navigateUp = navigator::pop,
                    onShowTrackerDialogClick = model::toggleTrackerSelectDialog,
                )
            },
        ) { contentPadding ->
            when {
                model.trackers.isEmpty() -> EmptyScreen(
                    message = stringResource(SYMR.strings.select_tracker),
                    modifier = Modifier.padding(contentPadding),
                )
                state.statusList.isEmpty() -> LoadingScreen(modifier = Modifier.padding(contentPadding))
                else -> {
                    val pagerState = rememberPagerState(
                        initialPage = state.currentTabIndex.coerceIn(0, state.statusList.lastIndex),
                        pageCount = { state.statusList.size },
                    )
                    LaunchedEffect(state.trackerId) {
                        scrollStates.clear()
                        pagerState.scrollToPage(0)
                    }
                    Column(modifier = Modifier.padding(contentPadding)) {
                        TrackStatusTabs(
                            statusList = state.statusList,
                            getStatusRes = state.getStatusRes,
                            pagerState = pagerState,
                        ) { index -> scope.launch { pagerState.animateScrollToPage(index) } }
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            val tab = state.tabs[page] ?: TabMangaList()
                            val scrollState = remember(page, state.trackerId) {
                                LazyListState(
                                    firstVisibleItemIndex = scrollStates[page]?.first ?: 0,
                                    firstVisibleItemScrollOffset = scrollStates[page]?.second ?: 0,
                                )
                            }
                            LaunchedEffect(page, scrollState, state.trackerId) {
                                snapshotFlow {
                                    val info = scrollState.layoutInfo
                                    info.visibleItemsInfo.lastOrNull()?.index?.let { it >= info.totalItemsCount - 20 } == true
                                }.collect { if (it) model.loadNextPage(page) }
                            }
                            LaunchedEffect(page, scrollState, state.trackerId) {
                                snapshotFlow { scrollState.firstVisibleItemIndex to scrollState.firstVisibleItemScrollOffset }
                                    .collect { scrollStates[page] = it }
                            }
                            LaunchedEffect(page, state.trackerId) { model.changeTab(page) }
                            if (tab.isLoading && tab.items.isEmpty()) {
                                LoadingScreen(modifier = Modifier.fillMaxWidth())
                                return@HorizontalPager
                            }
                            if (tab.items.isEmpty()) {
                                EmptyScreen(message = "All entries are in library.", modifier = Modifier.fillMaxSize())
                                return@HorizontalPager
                            }
                            FastScrollLazyColumn(
                                state = scrollState,
                                modifier = Modifier.fillMaxHeight(),
                                verticalArrangement = Arrangement.Top,
                            ) {
                                animeListItem(tab.items) { item ->
                                    navigator.push(GlobalSearchScreen(searchQuery = item.title.orEmpty()))
                                }
                                if (tab.isLoading) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            contentAlignment = Alignment.Center,
                                        ) { CircularProgressIndicator() }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (state.trackerSelectDialog && state.trackerId != null) {
            TrackerSelectDialog(
                trackers = model.trackers,
                onDismissRequest = model::toggleTrackerSelectDialog,
                onTrackerSelect = model::changeTracker,
                currentTrackerId = state.trackerId!!,
            )
        }
    }
}

@Composable
private fun TrackerMangaListAppBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    navigateUp: () -> Unit,
    onShowTrackerDialogClick: () -> Unit,
) {
    AppBar(
        navigateUp = navigateUp,
        titleContent = { Text(title, maxLines = 1) },
        actions = {
            IconButton(onClick = onShowTrackerDialogClick) {
                Icon(Icons.Default.Sync, contentDescription = stringResource(SYMR.strings.select_tracker))
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun TrackerSelectDialog(
    trackers: List<Tracker>,
    onDismissRequest: () -> Unit,
    onTrackerSelect: (Long) -> Unit,
    currentTrackerId: Long,
) {
    AlertDialog(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismissRequest,
        confirmButton = { TextButton(onClick = onDismissRequest) { Text(stringResource(MR.strings.action_cancel)) } },
        title = { Text(stringResource(SYMR.strings.select_tracker)) },
        text = {
            FlowRow(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                trackers.forEach { tracker ->
                    Box {
                        TrackLogoIcon(
                            tracker,
                            onClick = {
                                if (tracker.id != currentTrackerId) onTrackerSelect(tracker.id)
                            },
                        )
                        if (tracker.id == currentTrackerId) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.align(Alignment.TopEnd).size(16.dp),
                                tint = Color.Green,
                            )
                        }
                    }
                }
            }
        },
    )
}
