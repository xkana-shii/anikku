/*
 * Copyright 2024 Abdallah Mehiz
 * https://github.com/abdallahmehiz/mpvKt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.kanade.tachiyomi.ui.player.controls.components.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import eu.kanade.domain.track.model.AutoRereadResetMode
import eu.kanade.domain.track.service.TrackPreferences
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun RereadPromptDialog(
    currentEpisodeNumber: Float?,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val trackPreferences: TrackPreferences = remember { Injekt.get() }
    val resetMode: AutoRereadResetMode = trackPreferences.autoRereadResetMode().get()
    val chapterLabel: String = if (resetMode == AutoRereadResetMode.RESET_TO_ZERO) {
        stringResource(KMR.strings.chapter_label, "0")
    } else {
        currentEpisodeNumber?.let {
            val intPart = it.toInt()
            val display = if (it == intPart.toFloat()) intPart.toString() else it.toString()
            stringResource(KMR.strings.chapter_label, display)
        } ?: stringResource(KMR.strings.this_chapter_label)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        title = { Text(text = stringResource(KMR.strings.reread_prompt_title)) },
        text = { Text(text = stringResource(KMR.strings.reread_prompt_body, chapterLabel)) },
    )
}
