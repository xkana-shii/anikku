package eu.kanade.domain.track.service

import eu.kanade.domain.track.model.AutoRereadResetMode
import eu.kanade.domain.track.model.AutoTrackState
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import tachiyomi.domain.track.service.PreferredTrackerMap

class TrackPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun trackUsername(tracker: Tracker) = preferenceStore.getString(
        Preference.privateKey("pref_mangasync_username_${tracker.id}"),
        "",
    )

    fun trackPassword(tracker: Tracker) = preferenceStore.getString(
        Preference.privateKey("pref_mangasync_password_${tracker.id}"),
        "",
    )

    fun trackAuthExpired(tracker: Tracker) = preferenceStore.getBoolean(
        Preference.privateKey("pref_tracker_auth_expired_${tracker.id}"),
        false,
    )

    fun setCredentials(tracker: Tracker, username: String, password: String) {
        trackUsername(tracker).set(username)
        trackPassword(tracker).set(password)
        trackAuthExpired(tracker).set(false)
    }

    fun trackToken(tracker: Tracker) = preferenceStore.getString(Preference.privateKey("track_token_${tracker.id}"), "")

    fun anilistScoreType() = preferenceStore.getString("anilist_score_type", Anilist.POINT_10)

    fun autoUpdateTrack() = preferenceStore.getBoolean("pref_auto_update_manga_sync_key", true)

    fun trackOnAddingToLibrary() = preferenceStore.getBoolean("track_on_adding_to_library", true)

    fun showNextChapterAiringTime() = preferenceStore.getBoolean(
        "show_next_episode_airing_time",
        true,
    )

    fun autoUpdateTrackOnMarkRead() = preferenceStore.getEnum(
        "pref_auto_update_manga_on_mark_read",
        AutoTrackState.ALWAYS,
    )

    fun autoRereadBehavior() = preferenceStore.getEnum(
        "pref_auto_reread_behavior",
        AutoTrackState.ASK,
    )

    fun autoRereadResetMode() = preferenceStore.getEnum(
        "pref_auto_reread_reset_mode",
        AutoRereadResetMode.RESET_TO_CURRENT_EPISODE,
    )

    // AM -->
    fun smartTrackerSync() = preferenceStore.getBoolean("smart_sync_trackers", true)
    // <-- AM

    // KMK -->
    fun autoSyncProgressFromTrackers() = preferenceStore.getBoolean("pref_auto_sync_progress_from_trackers_key", true)
    // KMK <--

    // KMK --> Per-entry overrides are app state; the global priority is selected by long-pressing a service.
    fun preferredTrackerForAnime() =
        preferenceStore.getString(Preference.appStateKey("pref_preferred_tracker_for_anime"), "")

    private fun legacyPreferredTrackerOverrides() = preferenceStore.getString("pref_preferred_tracker_overrides", "")
    private val preferredTrackerLock = Any()

    fun getPreferredTrackerForAnime(animeId: Long): Long? =
        PreferredTrackerMap.decode(preferredTrackerForAnime().get())[animeId]
            ?: PreferredTrackerMap.decode(legacyPreferredTrackerOverrides().get())[animeId]

    fun setPreferredTrackerForAnime(animeId: Long, trackerId: Long?) = synchronized(preferredTrackerLock) {
        if (animeId > 0) {
            preferredTrackerForAnime().set(
                PreferredTrackerMap.update(preferredTrackerForAnime().get(), animeId, trackerId),
            )
            legacyPreferredTrackerOverrides().set(
                PreferredTrackerMap.update(legacyPreferredTrackerOverrides().get(), animeId, null),
            )
        }
    }

    fun priorityTrackerId() = preferenceStore.getLong("pref_priority_tracker_id", 0L)
    private fun legacyPriorityTrackerId() = preferenceStore.getLong("pref_priority_tracker", 0L)

    fun getPriorityTrackerId(): Long? =
        priorityTrackerId().get().takeIf { it > 0 } ?: legacyPriorityTrackerId().get().takeIf { it > 0 }

    fun setPriorityTrackerId(trackerId: Long?) {
        priorityTrackerId().set(trackerId?.takeIf { it > 0 } ?: 0L)
        legacyPriorityTrackerId().set(0L)
    }

    fun resolvePreferredTracker(animeId: Long, applicable: Set<Long>): Long? =
        getPreferredTrackerForAnime(animeId)?.takeIf { it in applicable }
            ?: getPriorityTrackerId()?.takeIf { it in applicable }
    // KMK <--
}
