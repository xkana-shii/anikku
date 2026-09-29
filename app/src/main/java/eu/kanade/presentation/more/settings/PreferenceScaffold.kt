package eu.kanade.presentation.more.settings

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.rememberTvAppBarContentFocusRequesters
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun PreferenceScaffold(
    titleRes: StringResource,
    actions: @Composable RowScope.() -> Unit = {},
    onBackPressed: (() -> Unit)? = null,
    itemsProvider: @Composable () -> List<Preference>,
) {
    val items = itemsProvider()
    val tvFocusRequesters = rememberTvAppBarContentFocusRequesters()
    val hasTvContentTarget = items.anyTvInitialFocusCandidate()
    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(titleRes),
                navigateUp = onBackPressed,
                actions = actions,
                scrollBehavior = it,
                tvFocusRequesters = tvFocusRequesters,
                tvContentAvailable = hasTvContentTarget,
            )
        },
        content = { contentPadding ->
            PreferenceScreen(
                items = items,
                contentPadding = contentPadding,
                tvFocusRequesters = tvFocusRequesters,
            )
        },
    )
}
