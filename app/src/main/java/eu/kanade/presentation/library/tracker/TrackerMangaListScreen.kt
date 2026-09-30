package eu.kanade.presentation.library.tracker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.i18n.ank.AMR

class TrackerMangaListScreen : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel { TrackerMangaListScreenModel() }
        val state by model.state.collectAsState()
        var showTrackerMenu by remember { mutableStateOf(false) }
        Scaffold(
            topBar = {
                AppBar(
                    title = model.getTrackerName(),
                    navigateUp = navigator::pop,
                    actions = {
                        IconButton(onClick = { showTrackerMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = showTrackerMenu,
                            onDismissRequest = { showTrackerMenu = false },
                        ) {
                            model.trackers.forEach { tracker ->
                                DropdownMenuItem(
                                    text = { Text(tracker.name) },
                                    onClick = {
                                        showTrackerMenu = false
                                        model.changeTracker(tracker.id)
                                    },
                                )
                            }
                        }
                    },
                )
            },
        ) { padding ->
            if (state.statusList.isEmpty()) return@Scaffold
            val tabIndex = state.currentTabIndex.coerceIn(0, state.statusList.lastIndex)
            Column(Modifier.fillMaxSize().padding(padding)) {
                ScrollableTabRow(selectedTabIndex = tabIndex) {
                    state.statusList.forEachIndexed { index, status ->
                        Tab(
                            selected = tabIndex == index,
                            onClick = { model.changeTab(index) },
                            text = { state.getStatusRes(status)?.let { Text(stringResource(it)) } },
                        )
                    }
                }
                val tab = state.tabs[tabIndex] ?: TabMangaList()
                LaunchedEffect(tabIndex, state.trackerId) { model.loadNextPage(tabIndex) }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(tab.items) { item ->
                        Button(
                            onClick = { navigator.push(GlobalSearchScreen(searchQuery = item.title.orEmpty())) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        ) { Text(item.title.orEmpty()) }
                    }
                    if (tab.isLoading) item { CircularProgressIndicator(Modifier.padding(16.dp)) }
                    if (!tab.endReached && tab.items.isNotEmpty()) item {
                        Button(onClick = { model.loadNextPage(tabIndex) }) { Text(stringResource(AMR.strings.action_load_more)) }
                    }
                }
            }
        }
    }
}
