package eu.kanade.tachiyomi.data.track.simkl

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SimklStatusTest {

    @Test
    fun `rewatching maps to the dedicated local Simkl status`() {
        toTrackStatus("rewatching") shouldBe Simkl.REWATCHING
    }
}
