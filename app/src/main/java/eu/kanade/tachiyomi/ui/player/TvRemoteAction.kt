package eu.kanade.tachiyomi.ui.player

import android.view.KeyEvent

internal enum class TvRemoteAction {
    ShowControls,
    SeekBackward,
    SeekForward,
    TogglePlayback,
    Play,
    Pause,
    PreviousEpisode,
    NextEpisode,
    HideControls,
    Stop,
}

/** Maps only keys the player should consume instead of the currently focused Compose control. */
internal fun tvRemoteAction(
    keyCode: Int,
    controlsShown: Boolean,
    modalOverlayShown: Boolean = false,
): TvRemoteAction? {
    if (modalOverlayShown && isTvNavigationKey(keyCode)) return null

    return when (keyCode) {
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_MENU,
        -> if (controlsShown) null else TvRemoteAction.ShowControls

        KeyEvent.KEYCODE_BACK -> if (controlsShown) TvRemoteAction.HideControls else null
        KeyEvent.KEYCODE_DPAD_LEFT -> if (controlsShown) null else TvRemoteAction.SeekBackward
        KeyEvent.KEYCODE_DPAD_RIGHT -> if (controlsShown) null else TvRemoteAction.SeekForward

        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        KeyEvent.KEYCODE_HEADSETHOOK,
        KeyEvent.KEYCODE_SPACE,
        -> TvRemoteAction.TogglePlayback
        KeyEvent.KEYCODE_MEDIA_PLAY -> TvRemoteAction.Play
        KeyEvent.KEYCODE_MEDIA_PAUSE -> TvRemoteAction.Pause
        KeyEvent.KEYCODE_MEDIA_PREVIOUS,
        KeyEvent.KEYCODE_CHANNEL_UP,
        -> TvRemoteAction.PreviousEpisode
        KeyEvent.KEYCODE_MEDIA_NEXT,
        KeyEvent.KEYCODE_CHANNEL_DOWN,
        -> TvRemoteAction.NextEpisode
        KeyEvent.KEYCODE_MEDIA_REWIND -> TvRemoteAction.SeekBackward
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> TvRemoteAction.SeekForward
        KeyEvent.KEYCODE_MEDIA_STOP -> TvRemoteAction.Stop
        else -> null
    }
}

internal fun isTvNavigationKey(keyCode: Int): Boolean {
    return keyCode == KeyEvent.KEYCODE_DPAD_UP ||
        keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
        keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
        keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
        keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
        keyCode == KeyEvent.KEYCODE_ENTER ||
        keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
        keyCode == KeyEvent.KEYCODE_MENU ||
        keyCode == KeyEvent.KEYCODE_BACK
}

internal fun shouldConsumeTvKeyUp(consumedKeyDown: Int?, keyUp: Int): Boolean {
    return consumedKeyDown == keyUp
}
