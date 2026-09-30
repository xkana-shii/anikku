package eu.kanade.tachiyomi.data.track.simkl

import eu.kanade.tachiyomi.data.database.models.Track

fun Track.toSimklStatus() = when (status) {
    Simkl.WATCHING -> "watching"
    Simkl.COMPLETED -> "completed"
    Simkl.ON_HOLD -> "hold"
    Simkl.NOT_INTERESTING -> "notinteresting"
    Simkl.PLAN_TO_WATCH -> "plantowatch"
    Simkl.REWATCHING -> "completed"
    else -> throw NotImplementedError("Unknown status: $status")
}

fun toTrackStatus(status: String) = when (status) {
    "watching" -> Simkl.WATCHING
    "completed" -> Simkl.COMPLETED
    "hold" -> Simkl.ON_HOLD
    "dropped", "notinteresting" -> Simkl.NOT_INTERESTING
    "plantowatch" -> Simkl.PLAN_TO_WATCH
    "rewatching" -> Simkl.REWATCHING
    else -> throw NotImplementedError("Unknown status: $status")
}

fun Long.toSimklListStatus() = when (this) {
    Simkl.WATCHING -> "watching"
    Simkl.COMPLETED -> "completed"
    Simkl.ON_HOLD -> "hold"
    Simkl.NOT_INTERESTING -> "notinteresting"
    Simkl.PLAN_TO_WATCH -> "plantowatch"
    else -> error("Simkl status $this does not have a list endpoint")
}
