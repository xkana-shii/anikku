package eu.kanade.domain.ui.model

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.ank.AMR

/** User override for the remote-first television UI policy. */
enum class TvUiMode(val titleRes: StringResource) {
    AUTOMATIC(AMR.strings.pref_tv_ui_mode_automatic),
    ALWAYS(AMR.strings.pref_tv_ui_mode_always),
    NEVER(AMR.strings.pref_tv_ui_mode_never),
}
