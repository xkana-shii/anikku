package eu.kanade.tachiyomi.ui.player.controls

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.ui.player.Dialogs
import eu.kanade.tachiyomi.ui.player.controls.components.dialogs.EpisodeListDialog
import eu.kanade.tachiyomi.ui.player.controls.components.dialogs.IntegerPickerDialog
import eu.kanade.tachiyomi.ui.player.controls.components.dialogs.RereadPromptDialog
import kotlinx.collections.immutable.ImmutableList

@Composable
fun PlayerDialogs(
    dialogShown: Dialogs,

    // Episode list
    episodeDisplayMode: Long?,
    currentEpisodeIndex: Int,
    episodeList: ImmutableList<Episode>,
    dateRelativeTime: Boolean,
    dateFormat: String,
    onBookmarkClicked: (Long?, Boolean) -> Unit,
    onFillermarkClicked: (Long?, Boolean) -> Unit,
    onEpisodeClicked: (Long?) -> Unit,
    onRereadConfirm: () -> Unit,

    onDismissRequest: () -> Unit,
) {
    when (dialogShown) {
        Dialogs.None -> {}
        Dialogs.EpisodeList -> {
            EpisodeListDialog(
                displayMode = episodeDisplayMode,
                currentEpisodeIndex = currentEpisodeIndex,
                episodeList = episodeList,
                dateRelativeTime = dateRelativeTime,
                dateFormat = dateFormat,
                onBookmarkClicked = onBookmarkClicked,
                onFillermarkClicked = onFillermarkClicked,
                onEpisodeClicked = onEpisodeClicked,
                onDismissRequest = onDismissRequest,
            )
        }
        Dialogs.RereadPrompt -> {
            RereadPromptDialog(
                currentEpisodeNumber = episodeList.getOrNull(currentEpisodeIndex)?.episode_number,
                onConfirm = onRereadConfirm,
                onDismissRequest = onDismissRequest,
            )
        }
        is Dialogs.IntegerPicker -> {
            IntegerPickerDialog(
                defaultValue = dialogShown.defaultValue,
                minValue = dialogShown.minValue,
                maxValue = dialogShown.maxValue,
                step = dialogShown.step,
                nameFormat = dialogShown.nameFormat,
                title = dialogShown.title,
                onChange = dialogShown.onChange,
                onDismissRequest = dialogShown.onDismissRequest,
            )
        }
    }
}
