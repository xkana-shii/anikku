package eu.kanade.domain.track.interactor

import eu.kanade.tachiyomi.data.track.Tracker
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.model.Track
import tachiyomi.domain.track.repository.TrackRepository

class TrackerBatchTest {
    @Test
    fun `one tracker failure does not cancel sibling and returned record persists`() = runBlocking {
        val failed = mockk<Tracker> { every { id } returns 1L }
        val successful = mockk<Tracker> { every { id } returns 2L }
        val repository = mockk<TrackRepository>()
        coJustRun { repository.insert(any()) }
        coEvery { successful.refresh(any()) } answers { firstArg() }
        val first = binding(1)
        val second = binding(2)

        val results = trackerBatch(listOf(failed to first, successful to second), InsertTrack(repository)) {
            if (service.id == 1L) error("first service failed")
            refresh()
        }

        results.first().error?.message shouldBe "first service failed"
        results.last().error shouldBe null
        results.last().track?.trackerId shouldBe 2L
        coVerify(exactly = 1) { repository.insert(match { it.trackerId == 2L }) }
    }

    private fun binding(trackerId: Long) = Track(
        id = trackerId,
        mangaId = 10,
        trackerId = trackerId,
        remoteId = 20,
        libraryId = null,
        title = "Example",
        lastChapterRead = 1.0,
        totalChapters = 12,
        status = 1,
        score = 0.0,
        remoteUrl = "",
        startDate = 0,
        finishDate = 0,
        private = false,
    )
}
