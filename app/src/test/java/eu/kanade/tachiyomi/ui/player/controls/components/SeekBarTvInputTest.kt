package eu.kanade.tachiyomi.ui.player.controls.components

import android.view.KeyEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SeekBarTvInputTest {

    @Test
    fun `initial dpad seek uses a large step`() {
        assertEquals(-30f, tvSeekStepSeconds(KeyEvent.KEYCODE_DPAD_LEFT, repeatCount = 0))
        assertEquals(30f, tvSeekStepSeconds(KeyEvent.KEYCODE_DPAD_RIGHT, repeatCount = 0))
    }

    @Test
    fun `held dpad seek uses fine-grained repeat steps`() {
        assertEquals(-5f, tvSeekStepSeconds(KeyEvent.KEYCODE_DPAD_LEFT, repeatCount = 1))
        assertEquals(5f, tvSeekStepSeconds(KeyEvent.KEYCODE_DPAD_RIGHT, repeatCount = 8))
    }

    @Test
    fun `non-horizontal keys are ignored`() {
        assertNull(tvSeekStepSeconds(KeyEvent.KEYCODE_DPAD_UP, repeatCount = 0))
    }
}
