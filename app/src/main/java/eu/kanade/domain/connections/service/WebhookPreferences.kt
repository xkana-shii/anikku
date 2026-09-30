package eu.kanade.domain.connections.service

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/** Private URL settings and per-event opt-in controls for best-effort webhooks. */
class WebhookPreferences(private val store: PreferenceStore) {
    fun enabled() = store.getBoolean("pref_webhook_enabled", false)
    fun discordUrl() = store.getString(Preference.privateKey("pref_webhook_discord_url"), "")
    fun genericUrl() = store.getString(Preference.privateKey("pref_webhook_generic_url"), "")
    fun event(event: WebhookEvent) = store.getBoolean("pref_webhook_${event.id}", false)
    fun events() = store.getStringSet("pref_webhook_events", emptySet())
    fun excludedCategories() = store.getStringSet("pref_webhook_excluded_categories", emptySet())
}

enum class WebhookEvent(val id: String) {
    EPISODE_STARTED("episode_started"),
    EPISODE_SEEN("episode_seen"),
    NEW_ANIME_STARTED("new_anime_started"),
    ANIME_FINISHED("anime_finished"),
    LIBRARY_ADDED("library_added"),
    LIBRARY_REMOVED("library_removed"),
    LIBRARY_UPDATE("library_update"),
    DOWNLOADS_FINISHED("downloads_finished"),
    BACKUP_CREATED("backup_created"),
    BACKUP_RESTORED("backup_restored"),
    ANIME_MIGRATED("anime_migrated"),
    APP_UPDATED("app_updated"),
    CAUGHT_UP("caught_up"),
    TEST("test"),
}
