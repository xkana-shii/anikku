package eu.kanade.tachiyomi.data.updater

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AppUpdateProgressTest {
    @Test
    fun `download progress uses absolute bytes including a resumed portion`() {
        downloadProgress(0, 100) shouldBe 0
        downloadProgress(40, 100) shouldBe 40
        downloadProgress(75, 100) shouldBe 75
        downloadProgress(100, 100) shouldBe 100
    }

    @Test
    fun `unknown size cannot show false determinate progress`() {
        downloadProgress(40, -1) shouldBe null
        downloadProgress(0, 0) shouldBe null
    }
}
