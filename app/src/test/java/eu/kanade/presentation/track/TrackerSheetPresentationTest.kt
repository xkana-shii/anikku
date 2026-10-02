package eu.kanade.presentation.track

import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.ui.manga.track.TrackItem
import eu.kanade.tachiyomi.util.lang.toLocalDate
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import tachiyomi.domain.track.model.Track
import java.time.format.DateTimeFormatter

class TrackerSheetPresentationTest {
    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    @Test
    fun `tracker count selects empty single and unified presentation data`() {
        val first = item(1)
        val second = item(2)
        val unbound = item(3).copy(track = null)

        val empty = TrackerSheetPresentation(listOf(unbound), null, dateFormat, emptySet())
        empty.bound.size shouldBe 0
        empty.visibleItems shouldBe listOf(unbound)

        val single = TrackerSheetPresentation(listOf(first, unbound), 1, dateFormat, emptySet())
        single.bound.size shouldBe 1
        single.primary shouldBe first

        val unified = TrackerSheetPresentation(listOf(first, second, unbound), 2, dateFormat, emptySet())
        unified.bound.size shouldBe 2
        unified.primary shouldBe second
    }

    @Test
    fun `shared editor uses preferred progress and another bound tracker for unsupported fields`() {
        val preferred = item(1, progress = 3.0, supportsScore = false)
        val scored = item(2, progress = 8.0, score = 7.0)
        val state = TrackerSheetPresentation(listOf(preferred, scored), 1, dateFormat, setOf(2))

        state.primary shouldBe preferred
        state.scoreItem shouldBe scored
        state.dateItem shouldBe scored
        state.score shouldBe "7.0"
        state.mismatchedIds shouldBe setOf(2L)
        state.errorIds shouldBe setOf(2L)
        state.startDate shouldBe dateFormat.format(scored.track!!.startDate.toLocalDate())
    }

    @Test
    fun `bound icon opens normally and rebinds in persistent edit mode`() {
        val bound = item(1)
        val unbound = item(2).copy(track = null)

        bound.unifiedIconClickAction(false) shouldBe UnifiedTrackerIconAction.OPEN
        bound.unifiedIconClickAction(true) shouldBe UnifiedTrackerIconAction.SEARCH
        bound.unifiedIconLongPressAction(true) shouldBe UnifiedTrackerIconAction.SET_PREFERRED
        unbound.unifiedIconClickAction(false) shouldBe UnifiedTrackerIconAction.SEARCH
    }

    private fun item(id: Long, progress: Double = 3.0, score: Double = 0.0, supportsScore: Boolean = true): TrackItem {
        val service = mockk<Tracker> {
            every { this@mockk.id } returns id
            every { getStatus(any()) } returns null
            every { getScoreList() } returns if (supportsScore) persistentListOf("0", "1") else persistentListOf()
            every { supportsReadingDates } returns supportsScore
            every { displayScore(any()) } returns score.toString()
        }
        val track = Track(id, 10, id, id, null, "Title", progress, 12, 2, score, "", 86400000, 172800000, false)
        return TrackItem(track, service)
    }
}
