package tachiyomi.domain.track.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.track.model.Track

class TrackerProgressSyncTest {
    private fun track(id: Long, progress: Double) =
        Track(id, 1, id, id, null, "Title", progress, 100, 1, 0.0, "", 10, 20, false)

    @Test
    fun `preferred progress wins only while its tracker is applicable`() {
        val tracks = listOf(track(1, 4.0), track(2, 9.5))

        TrackerProgressSync.progress(tracks, 1) shouldBe 4.0
        TrackerProgressSync.mismatchedIds(tracks, 1) shouldBe setOf(2L)
        TrackerProgressSync.progress(tracks, 999) shouldBe 9.5
        TrackerProgressSync.resolvePreferredTrack(tracks, 999) shouldBe tracks.last()
    }

    @Test
    fun `preferred tracker map clears invalid and malformed values`() {
        var raw = PreferredTrackerMap.update("{}", 10, 1)
        PreferredTrackerMap.decode(raw)[10] shouldBe 1L
        raw = PreferredTrackerMap.update(raw, 10, null)
        PreferredTrackerMap.decode(raw) shouldBe emptyMap()
        PreferredTrackerMap.decode("invalid") shouldBe emptyMap()
        PreferredTrackerMap.decode(" ".repeat(256_001)) shouldBe emptyMap()
        PreferredTrackerMap.decode("{\"bad\":1,\"-1\":2,\"3\":0,\"4\":2}") shouldBe mapOf(4L to 2L)
    }
}
