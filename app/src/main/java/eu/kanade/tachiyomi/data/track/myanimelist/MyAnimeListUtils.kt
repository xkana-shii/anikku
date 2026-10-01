package eu.kanade.tachiyomi.data.track.myanimelist

import eu.kanade.tachiyomi.data.database.models.Track

fun Track.toMyAnimeListStatus() = when (status) {
    MyAnimeList.WATCHING -> "watching"
    MyAnimeList.COMPLETED -> "completed"
    MyAnimeList.ON_HOLD -> "on_hold"
    MyAnimeList.DROPPED -> "dropped"
    MyAnimeList.PLAN_TO_WATCH -> "plan_to_watch"
    MyAnimeList.REWATCHING -> "watching"
    else -> null
}

fun getStatus(status: String?) = when (status) {
    "watching" -> MyAnimeList.WATCHING
    "completed" -> MyAnimeList.COMPLETED
    "on_hold" -> MyAnimeList.ON_HOLD
    "dropped" -> MyAnimeList.DROPPED
    "plan_to_watch" -> MyAnimeList.PLAN_TO_WATCH
    else -> MyAnimeList.WATCHING
}

internal data class MalRewatchUpdate(val isRewatching: Boolean, val completedCount: Int?)

internal fun malRewatchUpdate(status: Long, previouslyRewatching: Boolean, previousCount: Int): MalRewatchUpdate =
    if (status == MyAnimeList.COMPLETED && previouslyRewatching) {
        MalRewatchUpdate(isRewatching = false, completedCount = previousCount + 1)
    } else {
        MalRewatchUpdate(isRewatching = status == MyAnimeList.REWATCHING, completedCount = null)
    }
