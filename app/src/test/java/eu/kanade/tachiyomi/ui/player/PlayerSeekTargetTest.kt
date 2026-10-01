package eu.kanade.tachiyomi.ui.player

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PlayerSeekTargetTest {
    @Test
    fun `left and right seeks clamp without crossing episode boundaries`() {
        boundedSeekTarget(3.0, 100.0, -10) shouldBe 0
        boundedSeekTarget(20.0, 100.0, -10) shouldBe 10
        boundedSeekTarget(20.0, 100.0, 10) shouldBe 30
        boundedSeekTarget(95.0, 100.0, 10) shouldBe 99
    }
}
