package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.connections.service.WebhookEvent
import eu.kanade.domain.connections.service.WebhookPreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.data.webhook.WebhookNotifier
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CancellationException
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.i18n.ank.AMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsWebhookScreen : SearchableSettings {
    private fun readResolve(): Any = SettingsWebhookScreen

    @Composable
    override fun getTitleRes() = AMR.strings.pref_webhooks

    @Composable
    override fun getPreferences(): List<Preference> {
        val preferences = remember { Injekt.get<WebhookPreferences>() }
        val categories by remember { Injekt.get<GetCategories>().subscribe() }.collectAsState(emptyList())
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        var testing by remember { mutableStateOf(false) }
        val connections = listOf(
            Preference.PreferenceItem.SwitchPreference(
                preferences.enabled(),
                stringResource(AMR.strings.pref_webhooks_enabled),
            ),
            Preference.PreferenceItem.EditTextPreference(
                preference = preferences.discordUrl(),
                title = stringResource(AMR.strings.pref_webhooks_discord_url),
                subtitle = null,
                onValueChanged = { it.isBlank() || it.toHttpUrlOrNull() != null },
            ),
            Preference.PreferenceItem.EditTextPreference(
                preference = preferences.genericUrl(),
                title = stringResource(AMR.strings.pref_webhooks_generic_url),
                subtitle = null,
                onValueChanged = { it.isBlank() || it.toHttpUrlOrNull() != null },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(AMR.strings.pref_webhooks_test),
                onClick = if (testing) {
                    null
                } else {
                    {
                        testing = true
                        scope.launchIO {
                            try {
                                Injekt.get<WebhookNotifier>().sendTest()
                                withUIContext { context.toast(AMR.strings.webhook_test_sent) }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                withUIContext { context.toast(with(context) { e.formattedMessage }) }
                            } finally {
                                withUIContext { testing = false }
                            }
                        }
                    }
                },
            ),
            Preference.PreferenceItem.InfoPreference(stringResource(AMR.strings.webhooks_privacy)),
        )
        val events = WebhookEvent.entries.filterNot { it == WebhookEvent.TEST }.map { event ->
            Preference.PreferenceItem.SwitchPreference(
                preference = preferences.event(event),
                title = stringResource(event.title),
            )
        }
        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(AMR.strings.pref_webhooks),
                preferenceItems = connections.toImmutableList(),
            ),
            Preference.PreferenceGroup(
                title = stringResource(AMR.strings.pref_webhooks_events),
                preferenceItems = events.toImmutableList(),
            ),
            Preference.PreferenceGroup(
                title = stringResource(AMR.strings.pref_webhooks_excluded_categories),
                preferenceItems = listOf(
                    Preference.PreferenceItem.MultiSelectListPreference(
                        preference = preferences.excludedCategories(),
                        title = stringResource(AMR.strings.pref_webhooks_excluded_categories),
                        entries = categories.filterNot { it.isSystemCategory }.sortedBy { it.order }
                            .associate { it.id.toString() to it.name }.toImmutableMap(),
                    ),
                    Preference.PreferenceItem.InfoPreference(
                        stringResource(AMR.strings.webhook_exclusions_help),
                    ),
                ).toImmutableList(),
            ),
        )
    }
}

private val WebhookEvent.title: StringResource
    get() = when (this) {
        WebhookEvent.EPISODE_STARTED -> AMR.strings.webhook_episode_started
        WebhookEvent.EPISODE_SEEN -> AMR.strings.webhook_episode_seen
        WebhookEvent.NEW_ANIME_STARTED -> AMR.strings.webhook_new_anime_started
        WebhookEvent.ANIME_FINISHED -> AMR.strings.webhook_anime_finished
        WebhookEvent.LIBRARY_ADDED -> AMR.strings.webhook_anime_added
        WebhookEvent.LIBRARY_REMOVED -> AMR.strings.webhook_anime_removed
        WebhookEvent.LIBRARY_UPDATE -> AMR.strings.webhook_library_update
        WebhookEvent.DOWNLOADS_FINISHED -> AMR.strings.webhook_downloads_finished
        WebhookEvent.BACKUP_CREATED -> AMR.strings.webhook_backup_created
        WebhookEvent.BACKUP_RESTORED -> AMR.strings.webhook_backup_restored
        WebhookEvent.ANIME_MIGRATED -> AMR.strings.webhook_anime_migrated
        WebhookEvent.APP_UPDATED -> AMR.strings.webhook_app_updated
        WebhookEvent.CAUGHT_UP -> AMR.strings.webhook_anime_caught_up
        WebhookEvent.TEST -> AMR.strings.pref_webhooks_test
    }
