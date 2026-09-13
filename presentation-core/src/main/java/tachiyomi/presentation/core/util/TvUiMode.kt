package tachiyomi.presentation.core.util

import androidx.compose.runtime.compositionLocalOf

/** Resolved by the app layer so shared components use one TV-navigation policy. */
val LocalTvUiEnabled = compositionLocalOf { false }
