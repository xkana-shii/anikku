package eu.kanade.domain.track.interactor

import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.tachiyomi.data.track.Tracker
import tachiyomi.domain.track.model.Track

/**
 * Performs the remote transition before persisting the returned tracker record.
 * A local-only status change is not a successful rewatch.
 */
internal suspend fun startRewatchOnTracker(
    track: Track,
    service: Tracker,
    progress: Double,
    startedAt: Long,
    persist: suspend (Track) -> Unit,
) {
    val refreshed = requireNotNull(service.refresh(track.toDbTrack()).toDomainTrack(idRequired = true))
    val outgoing = refreshed.copy(
        status = service.getRereadingStatus(),
        lastChapterRead = progress,
        startDate = startedAt,
        finishDate = 0L,
    )
    val returned = requireNotNull(
        service.update(outgoing.toDbTrack(), didReadChapter = false).toDomainTrack(idRequired = true),
    )
    check(returned.status == service.getRereadingStatus()) {
        "Tracker did not confirm the rewatch status"
    }
    persist(returned)
}
