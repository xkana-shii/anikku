package eu.kanade.domain.track.interactor

import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import eu.kanade.tachiyomi.data.track.shikimori.Shikimori
import eu.kanade.tachiyomi.data.track.simkl.Simkl
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import tachiyomi.domain.track.model.Track

class StartRewatchTest {
    @Test
    fun `rewatch sends and persists tracker-specific status and reset progress`() = runBlocking {
        listOf(
            MyAnimeList.REWATCHING,
            Anilist.REWATCHING,
            Shikimori.REWATCHING,
            Simkl.REWATCHING,
        ).forEachIndexed { index, status ->
            val service = mockk<Tracker>()
            every { service.getRereadingStatus() } returns status
            coEvery { service.refresh(any()) } answers { firstArg() }
            coEvery { service.update(any(), false) } answers { firstArg() }
            val binding = binding(index.toLong())
            var persisted: Track? = null

            startRewatchOnTracker(binding, service, progress = 4.0, startedAt = 123456789L) {
                persisted = it
            }

            coVerify(exactly = 1) {
                service.update(
                    match {
                        it.status == status &&
                            it.last_episode_seen == 4.0
                    },
                    false,
                )
            }
            persisted?.status shouldBe status
            persisted?.lastChapterRead shouldBe 4.0
        }
    }

    private fun binding(trackerId: Long) = Track(
        id = trackerId + 1,
        mangaId = 10,
        trackerId = trackerId,
        remoteId = 20,
        libraryId = null,
        title = "Example",
        lastChapterRead = 12.0,
        totalChapters = 12,
        status = MyAnimeList.COMPLETED,
        score = 8.0,
        remoteUrl = "",
        startDate = 1,
        finishDate = 2,
        private = false,
    )
}
