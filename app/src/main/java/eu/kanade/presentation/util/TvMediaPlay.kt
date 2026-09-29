package eu.kanade.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import android.view.KeyEvent as AndroidKeyEvent

/** Handles the initial TV media-play press on an existing focused action. */
@Composable
fun Modifier.tvMediaPlayAction(action: (() -> Unit)?): Modifier = composed {
    if (!isTvUi() || action == null) return@composed this
    onPreviewKeyEvent { event ->
        val keyCode = event.nativeKeyEvent.keyCode
        if (
            event.type == KeyEventType.KeyDown &&
            event.nativeKeyEvent.repeatCount == 0 &&
            keyCode in TV_PLAY_KEY_CODES
        ) {
            action()
            true
        } else {
            false
        }
    }
}

private val TV_PLAY_KEY_CODES = setOf(
    AndroidKeyEvent.KEYCODE_MEDIA_PLAY,
    AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
)
