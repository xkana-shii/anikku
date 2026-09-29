package eu.kanade.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import eu.kanade.presentation.util.isTvUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.TabText
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.focusHighlight
import tachiyomi.presentation.core.util.tvFocusable

object TabbedDialogPaddings {
    val Horizontal = 24.dp
    val Vertical = 8.dp
}

internal data class TabbedDialogPageFocus(
    val firstRequester: FocusRequester,
    val tabRequester: FocusRequester,
)

internal val LocalTabbedDialogPageFocus = compositionLocalOf<TabbedDialogPageFocus?> { null }

@Composable
fun Modifier.tabbedDialogFirstFocusTarget(): Modifier {
    val pageFocus = LocalTabbedDialogPageFocus.current ?: return this
    return focusRequester(pageFocus.firstRequester)
        .focusProperties { up = pageFocus.tabRequester }
}

@Composable
fun TabbedDialog(
    onDismissRequest: () -> Unit,
    tabTitles: ImmutableList<String>,
    modifier: Modifier = Modifier,
    tabOverflowMenuContent: (@Composable ColumnScope.(() -> Unit) -> Unit)? = null,
    pagerState: PagerState = rememberPagerState { tabTitles.size },
    content: @Composable (Int) -> Unit,
) {
    AdaptiveSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        val scope = rememberCoroutineScope()
        val isTvUi = isTvUi()
        val tabFocusRequesters = remember(tabTitles) { tabTitles.map { FocusRequester() } }
        val pageFocusRequesters = remember(tabTitles) { tabTitles.map { FocusRequester() } }
        var tvPage by rememberSaveable { mutableStateOf(pagerState.currentPage) }
        val selectedPage = if (isTvUi) tvPage else pagerState.currentPage
        val tvPageStateHolder = rememberSaveableStateHolder()

        LaunchedEffect(isTvUi) {
            if (isTvUi) {
                withFrameNanos { }
                pageFocusRequesters.firstOrNull()?.requestFocus()
            }
        }

        Column {
            Row {
                PrimaryTabRow(
                    modifier = Modifier.weight(1f),
                    selectedTabIndex = selectedPage,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    divider = {},
                ) {
                    tabTitles.fastForEachIndexed { index, tab ->
                        val interactionSource = remember(index) { MutableInteractionSource() }
                        Tab(
                            modifier = Modifier
                                .focusRequester(tabFocusRequesters[index])
                                .focusProperties {
                                    if (isTvUi) down = pageFocusRequesters[index]
                                }
                                .tvFocusable(interactionSource),
                            selected = selectedPage == index,
                            onClick = {
                                if (isTvUi) {
                                    tvPage = index
                                } else {
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                }
                            },
                            text = { TabText(text = tab) },
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                            interactionSource = interactionSource,
                        )
                    }
                }

                tabOverflowMenuContent?.let { MoreMenu(it) }
            }
            HorizontalDivider()

            if (isTvUi) {
                Box(modifier = Modifier.animateContentSize()) {
                    tvPageStateHolder.SaveableStateProvider(selectedPage) {
                        CompositionLocalProvider(
                            LocalTabbedDialogPageFocus provides TabbedDialogPageFocus(
                                firstRequester = pageFocusRequesters[selectedPage],
                                tabRequester = tabFocusRequesters[selectedPage],
                            ),
                        ) {
                            content(selectedPage)
                        }
                    }
                }
            } else {
                HorizontalPager(
                    modifier = Modifier.animateContentSize(),
                    state = pagerState,
                    verticalAlignment = Alignment.Top,
                    pageContent = { page -> content(page) },
                )
            }
        }
    }
}

@Composable
private fun MoreMenu(
    content: @Composable ColumnScope.(() -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(modifier = Modifier.focusHighlight(), onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(MR.strings.label_more),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            content { expanded = false }
        }
    }
}
