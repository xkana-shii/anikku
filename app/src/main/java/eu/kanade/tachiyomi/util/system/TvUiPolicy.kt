package eu.kanade.tachiyomi.util.system

import eu.kanade.domain.ui.model.TvUiMode

/** Resolves the user policy independently from raw device capability detection. */
internal fun resolveTvUiEnabled(mode: TvUiMode, isTelevision: Boolean): Boolean {
    return when (mode) {
        TvUiMode.AUTOMATIC -> isTelevision
        TvUiMode.ALWAYS -> true
        TvUiMode.NEVER -> false
    }
}
