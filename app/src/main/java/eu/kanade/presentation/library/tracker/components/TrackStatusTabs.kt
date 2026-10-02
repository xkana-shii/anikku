package eu.kanade.presentation.library.tracker.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.icerock.moko.resources.StringResource
import tachiyomi.presentation.core.components.material.TabText
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TrackStatusTabs(
    statusList: List<Long>,
    getStatusRes: (Long) -> StringResource?,
    pagerState: PagerState,
    onTabItemClick: (Int) -> Unit,
) {
    Column(modifier = Modifier.zIndex(1f)) {
        PrimaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage.coerceAtMost(statusList.lastIndex),
            edgePadding = 0.dp,
            divider = {},
        ) {
            statusList.forEachIndexed { index, status ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { onTabItemClick(index) },
                    text = { TabText(text = getStatusRes(status)?.let { stringResource(it) }.orEmpty()) },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        HorizontalDivider()
    }
}
