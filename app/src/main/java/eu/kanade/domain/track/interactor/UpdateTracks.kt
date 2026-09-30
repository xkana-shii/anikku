package eu.kanade.domain.track.interactor

import eu.kanade.tachiyomi.data.track.TrackerManager
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.model.Track

/**
 * Shared multi-tracker writer used by tracker UI callers. A failed service is
 * represented in the result and never cancels a sibling service write.
 */
class UpdateTracks(
    private val trackerManager: TrackerManager,
    private val insertTrack: InsertTrack,
) {
    suspend fun update(
        tracks: List<Track>,
        transform: (Track) -> Track?,
        didReadEpisode: Boolean = false,
    ): List<TrackerBatchResult> = trackerBatch(
        entries = tracks.mapNotNull { track -> trackerManager.get(track.trackerId)?.let { it to track } },
        insertTrack = insertTrack,
    ) { tracker, track ->
        val edited = transform(track) ?: track
        tracker.updateDomain(edited, didReadEpisode)
    }
}
