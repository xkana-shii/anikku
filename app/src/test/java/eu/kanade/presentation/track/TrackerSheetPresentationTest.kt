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
