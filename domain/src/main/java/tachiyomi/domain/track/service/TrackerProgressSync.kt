package tachiyomi.domain.track.service

import tachiyomi.domain.track.model.Track
import kotlin.math.abs

/** Pure preferred-progress selection shared by player and tracker UI. */
object TrackerProgressSync {
    fun resolvePreferredTrack(tracks: List<Track>, preferredId: Long?): Track? =
        tracks.find { it.trackerId == preferredId }
            ?: tracks.filter { it.lastChapterRead.isFinite() && it.lastChapterRead >= 0 }.maxByOrNull { it.lastChapterRead }
            ?: tracks.firstOrNull()

    fun progress(tracks: List<Track>, preferredId: Long?): Double =
        tracks.find { it.trackerId == preferredId }?.lastChapterRead?.takeIf { it.isFinite() && it >= 0 }
            ?: tracks.map { it.lastChapterRead }.filter { it.isFinite() && it >= 0 }.maxOrNull()
            ?: 0.0

    fun mismatchedIds(tracks: List<Track>, preferredId: Long?): Set<Long> {
        val progress = progress(tracks, preferredId)
        return tracks.filter { abs(it.lastChapterRead - progress) > 0.01 }.map { it.trackerId }.toSet()
    }
}
