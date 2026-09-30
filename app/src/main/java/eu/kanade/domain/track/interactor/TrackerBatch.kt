package eu.kanade.domain.track.interactor

import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.tachiyomi.data.track.Tracker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.model.Track

/** Executes remote tracker writes independently, then persists successful responses. */
internal suspend fun trackerBatch(
    entries: List<Pair<Tracker, Track>>,
    insertTrack: InsertTrack,
    operation: suspend (Tracker, Track) -> Track,
): List<TrackerBatchResult> = supervisorScope {
    entries.distinctBy { it.first.id }.map { (tracker, track) ->
        async {
            try {
                val returned = operation(tracker, track)
                insertTrack.await(returned)
                TrackerBatchResult(tracker.id, returned, null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                TrackerBatchResult(tracker.id, null, e)
            }
        }
    }.awaitAll()
}

data class TrackerBatchResult(val trackerId: Long, val track: Track?, val error: Throwable?)

internal suspend fun Tracker.refreshDomain(track: Track): Track = refresh(track.toDbTrack()).toDomainTrack()!!

internal suspend fun Tracker.updateDomain(track: Track, didReadEpisode: Boolean = false): Track =
    update(track.toDbTrack(), didReadEpisode).toDomainTrack()!!
