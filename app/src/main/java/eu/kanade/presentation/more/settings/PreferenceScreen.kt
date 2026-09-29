package eu.kanade.presentation.more.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import eu.kanade.presentation.components.TvAppBarContentFocusRequesters
import eu.kanade.presentation.components.tvAppBarContentFocusTarget
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.presentation.more.settings.widget.PreferenceGroupHeader
import eu.kanade.presentation.util.isTvUi
import kotlinx.coroutines.delay
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import kotlin.time.Duration.Companion.seconds

/**
 * Preference Screen composable which contains a list of [Preference] items
 * @param items [Preference] items which should be displayed on the preference screen. An item can be a single [PreferenceItem] or a group ([Preference.PreferenceGroup])
 * @param modifier [Modifier] to be applied to the preferenceScreen layout
 */
@Composable
fun PreferenceScreen(
    items: List<Preference>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    tvFocusRequesters: TvAppBarContentFocusRequesters? = null,
) {
    val state = rememberLazyListState()
    val isTvUi = isTvUi()
    val initialItemFocusRequester = remember { FocusRequester() }
    var initialFocusRequested by rememberSaveable { mutableStateOf(false) }
    val initialFocusItem = items.asSequence()
        .flatMap { preference ->
            when (preference) {
                is Preference.PreferenceGroup -> preference.preferenceItems.asSequence()
                is Preference.PreferenceItem<*, *> -> sequenceOf(preference)
            }
        }
        .firstOrNull { it.isTvInitialFocusCandidate() }
    LaunchedEffect(isTvUi, initialFocusItem, initialFocusRequested) {
        if (isTvUi && initialFocusItem != null && !initialFocusRequested) {
            withFrameNanos { }
            (tvFocusRequesters?.content ?: initialItemFocusRequester).requestFocus()
            initialFocusRequested = true
        }
    }
    val highlightKey = SearchableSettings.highlightKey
    if (highlightKey != null) {
        LaunchedEffect(Unit) {
            val i = items.findHighlightedIndex(highlightKey)
            if (i >= 0) {
                delay(0.5.seconds)
                state.animateScrollToItem(i)
            }
            SearchableSettings.highlightKey = null
        }
    }

    ScrollbarLazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
    ) {
        items.fastForEachIndexed { i, preference ->
            when (preference) {
                // Create Preference Group
                is Preference.PreferenceGroup -> {
                    if (!preference.enabled) return@fastForEachIndexed

                    item {
                        Column {
                            PreferenceGroupHeader(title = preference.title)
                        }
                    }
                    items(preference.preferenceItems) { item ->
                        PreferenceItem(
                            item = item,
                            highlightKey = highlightKey,
                            modifier = if (isTvUi && item === initialFocusItem) {
                                tvFocusRequesters?.let { Modifier.tvAppBarContentFocusTarget(it) }
                                    ?: Modifier.focusRequester(initialItemFocusRequester)
                            } else {
                                Modifier
                            },
                        )
                    }
                    item {
                        if (i < items.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                // Create Preference Item
                is Preference.PreferenceItem<*, *> -> item {
                    PreferenceItem(
                        item = preference,
                        highlightKey = highlightKey,
                        modifier = if (isTvUi && preference === initialFocusItem) {
                            tvFocusRequesters?.let { Modifier.tvAppBarContentFocusTarget(it) }
                                ?: Modifier.focusRequester(initialItemFocusRequester)
                        } else {
                            Modifier
                        },
                    )
                }
            }
        }
    }
}

private fun Preference.PreferenceItem<*, *>.isTvInitialFocusCandidate(): Boolean {
    if (!enabled) return false
    return when (this) {
        is Preference.PreferenceItem.InfoPreference,
        is Preference.PreferenceItem.CustomPreference,
        is Preference.PreferenceItem.SliderPreference,
        -> false
        is Preference.PreferenceItem.TextPreference -> onClick != null
        else -> true
    }
}

internal fun List<Preference>.anyTvInitialFocusCandidate(): Boolean {
    return asSequence()
        .flatMap { preference ->
            when (preference) {
                is Preference.PreferenceGroup -> preference.preferenceItems.asSequence()
                is Preference.PreferenceItem<*, *> -> sequenceOf(preference)
            }
        }
        .any { it.isTvInitialFocusCandidate() }
}

private fun List<Preference>.findHighlightedIndex(highlightKey: String): Int {
    return flatMap {
        if (it is Preference.PreferenceGroup) {
            buildList<String?> {
                add(null) // Header
                addAll(it.preferenceItems.map { groupItem -> groupItem.title })
                add(null) // Spacer
            }
        } else {
            listOf(it.title)
        }
    }.indexOfFirst { it == highlightKey }
}
