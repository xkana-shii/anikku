package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.track.model.AutoRereadResetMode
import eu.kanade.domain.track.model.AutoTrackState
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.PreferenceScreen
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import tachiyomi.i18n.ank.AMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Komikku-style settings presentation with Anikku's rewatch preferences and semantics. */
object AutoRewatchSettingsScreen : Screen {
    private fun readResolve(): Any = AutoRewatchSettingsScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val preferences = Injekt.get<TrackPreferences>()
        val items = listOf(
            Preference.PreferenceItem.ListPreference(
                preference = preferences.autoRereadBehavior(),
                entries = persistentMapOf(
                    AutoTrackState.ALWAYS to stringResource(AMR.strings.pref_auto_rewatch_on),
                    AutoTrackState.ASK to stringResource(AMR.strings.pref_auto_rewatch_ask),
                    AutoTrackState.NEVER to stringResource(AMR.strings.pref_auto_rewatch_off),
                ),
                title = stringResource(AMR.strings.pref_auto_reread_behavior_title),
            ),
            Preference.PreferenceItem.ListPreference(
                preference = preferences.autoRereadResetMode(),
                entries = AutoRereadResetMode.entries.associateWith { stringResource(it.titleRes) }.toPersistentMap(),
                title = stringResource(AMR.strings.pref_auto_reread_reset_to),
            ),
        )
        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(AMR.strings.pref_auto_reread_behavior),
                    navigateUp = navigator::pop,
                )
            },
        ) { paddingValues ->
            PreferenceScreen(items = items, contentPadding = paddingValues)
        }
    }
}
