package eu.kanade.tachiyomi.data.track

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.anilist.toApiStatus
import eu.kanade.tachiyomi.data.track.myanimelist.MalRewatchUpdate
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import eu.kanade.tachiyomi.data.track.myanimelist.malRewatchUpdate
import eu.kanade.tachiyomi.data.track.myanimelist.toMyAnimeListStatus
import eu.kanade.tachiyomi.data.track.shikimori.Shikimori
import eu.kanade.tachiyomi.data.track.shikimori.toShikimoriStatus
import eu.kanade.tachiyomi.data.track.simkl.Simkl
import eu.kanade.tachiyomi.data.track.simkl.toSimklStatus
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class RewatchStatusMappingTest {
    @Test
    fun `repeat states map to each remote service's representation`() {
        Track.create(0).apply { status = MyAnimeList.REWATCHING }.toMyAnimeListStatus() shouldBe "watching"
        Track.create(0).apply { status = Anilist.REWATCHING }.toApiStatus() shouldBe "REPEATING"
        Track.create(0).apply { status = Shikimori.REWATCHING }.toShikimoriStatus() shouldBe "rewatching"
        Track.create(0).apply { status = Simkl.REWATCHING }.toSimklStatus() shouldBe "completed"
    }

    @Test
    fun `MAL starts rewatch without increment and increments only on completion`() {
        malRewatchUpdate(MyAnimeList.REWATCHING, previouslyRewatching = false, previousCount = 2) shouldBe
            MalRewatchUpdate(isRewatching = true, completedCount = null)
        malRewatchUpdate(MyAnimeList.COMPLETED, previouslyRewatching = true, previousCount = 2) shouldBe
            MalRewatchUpdate(isRewatching = false, completedCount = 3)
        malRewatchUpdate(MyAnimeList.COMPLETED, previouslyRewatching = false, previousCount = 2) shouldBe
            MalRewatchUpdate(isRewatching = false, completedCount = null)
    }
}
