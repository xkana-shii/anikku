package eu.kanade.tachiyomi.ui.player

import android.view.KeyEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TvRemoteActionTest {

    @Test
    fun `hidden controls expose themselves before accepting navigation`() {
        assertEquals(
            TvRemoteAction.ShowControls,
            tvRemoteAction(KeyEvent.KEYCODE_DPAD_CENTER, controlsShown = false),
        )
        assertEquals(
            TvRemoteAction.SeekBackward,
            tvRemoteAction(KeyEvent.KEYCODE_DPAD_LEFT, controlsShown = false),
        )
        assertEquals(
            TvRemoteAction.SeekForward,
            tvRemoteAction(KeyEvent.KEYCODE_DPAD_RIGHT, controlsShown = false),
        )
    }

    @Test
    fun `visible controls release navigation to Compose focus`() {
        listOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
        ).forEach { keyCode ->
            assertNull(tvRemoteAction(keyCode, controlsShown = true))
        }
    }

    @Test
    fun `modal overlays receive every navigation key`() {
        listOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_BACK,
        ).forEach { keyCode ->
            assertNull(
                tvRemoteAction(
                    keyCode = keyCode,
                    controlsShown = false,
                    modalOverlayShown = true,
                ),
            )
        }
    }

    @Test
    fun `media keys keep their player actions`() {
        assertEquals(TvRemoteAction.TogglePlayback, tvRemoteAction(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, false))
        assertEquals(TvRemoteAction.PreviousEpisode, tvRemoteAction(KeyEvent.KEYCODE_MEDIA_PREVIOUS, true))
        assertEquals(TvRemoteAction.NextEpisode, tvRemoteAction(KeyEvent.KEYCODE_MEDIA_NEXT, true))
        assertEquals(TvRemoteAction.Stop, tvRemoteAction(KeyEvent.KEYCODE_MEDIA_STOP, true))
    }

    @Test
    fun `only a matching handled key-up is consumed`() {
        assertTrue(shouldConsumeTvKeyUp(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_DPAD_CENTER))
        assertFalse(shouldConsumeTvKeyUp(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_DPAD_LEFT))
        assertFalse(shouldConsumeTvKeyUp(null, KeyEvent.KEYCODE_DPAD_CENTER))
    }
}
