package eu.kanade.tachiyomi.ui.player.controls.components.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import eu.kanade.domain.track.model.AutoRereadResetMode
import eu.kanade.domain.track.service.TrackPreferences
import tachiyomi.i18n.MR
import tachiyomi.i18n.ank.AMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun RereadPromptDialog(
    currentEpisodeNumber: Float?,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val resetMode = Injekt.get<TrackPreferences>().autoRereadResetMode().get()
    val label = if (resetMode == AutoRereadResetMode.RESET_TO_ZERO) {
        "0"
    } else {
        currentEpisodeNumber?.toString() ?: stringResource(AMR.strings.this_episode)
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(MR.strings.action_ok)) } },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(stringResource(MR.strings.action_cancel)) } },
        title = { Text(stringResource(AMR.strings.reread_prompt_title)) },
        text = { Text(stringResource(AMR.strings.reread_prompt_body, label)) },
    )
}
