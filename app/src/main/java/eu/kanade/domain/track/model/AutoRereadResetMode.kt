package eu.kanade.domain.track.model

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.ank.AMR

enum class AutoRereadResetMode(val titleRes: StringResource) {
    RESET_TO_ZERO(AMR.strings.pref_auto_reread_reset_to_zero),
    RESET_TO_CURRENT_EPISODE(AMR.strings.pref_auto_reread_reset_to_current_episode),
}
