package eu.kanade.presentation.more.settings.screen.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.WarningBanner
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.backup.create.BackupCreateJob
import eu.kanade.tachiyomi.data.backup.create.BackupCreator
import eu.kanade.tachiyomi.data.backup.create.BackupEntryFilter
import eu.kanade.tachiyomi.data.backup.create.BackupOptions
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.update
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.i18n.ank.AMR
import tachiyomi.presentation.core.components.LabeledCheckbox
import tachiyomi.presentation.core.components.LazyColumnWithAction
import tachiyomi.presentation.core.components.SectionCard
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CreateBackupScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel { CreateBackupScreenModel() }
        val state by model.state.collectAsState()
        val getCategories = remember { Injekt.get<GetCategories>() }
        val categories by getCategories.subscribe().collectAsState(initial = emptyList())

        val chooseBackupDir = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/*"),
        ) {
            if (it != null) {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                model.createBackup(context, it)
                navigator.pop()
            }
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.pref_create_backup),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            LazyColumnWithAction(
                contentPadding = contentPadding,
                actionLabel = stringResource(MR.strings.action_create),
                actionEnabled = state.options.canCreate(),
                onClickAction = {
                    if (!BackupCreateJob.isManualJobRunning(context)) {
                        try {
                            chooseBackupDir.launch(BackupCreator.getFilename())
                        } catch (_: ActivityNotFoundException) {
                            context.toast(MR.strings.file_picker_error)
                        }
                    } else {
                        context.toast(MR.strings.backup_in_progress)
                    }
                },
            ) {
                if (DeviceUtil.isMiui && DeviceUtil.isMiuiOptimizationDisabled()) {
                    item {
                        WarningBanner(MR.strings.restore_miui_warning)
                    }
                }

                item {
                    SectionCard(MR.strings.label_library) {
                        Options(BackupOptions.libraryOptions, state, model)
                    }
                }

                item {
                    SectionCard(MR.strings.categories) {
                        BackupCategories(categories.filterNot { it.isSystemCategory }, state, model)
                    }
                }

                item {
                    SectionCard(MR.strings.label_settings) {
                        Options(BackupOptions.settingsOptions, state, model)
                    }
                }

                item {
                    SectionCard(MR.strings.label_extensions) {
                        Options(BackupOptions.extensionOptions, state, model)
                    }
                }
            }
        }
    }

    @Composable
    private fun BackupCategories(
        categories: List<Category>,
        state: CreateBackupScreenModel.State,
        model: CreateBackupScreenModel,
    ) {
        LabeledCheckbox(
            label = stringResource(AMR.strings.backup_all_categories),
            checked = !state.filterLibraryEntries,
            onCheckedChange = { model.selectAllCategories(it, categories) },
            enabled = state.options.libraryEntries,
        )
        categories.forEach { category ->
            LabeledCheckbox(
                label = category.visualName,
                checked = !state.filterLibraryEntries || category.id in state.categoryIds,
                onCheckedChange = { model.toggleCategory(category.id, it, categories) },
                enabled = state.options.libraryEntries,
            )
        }
        LabeledCheckbox(
            label = stringResource(AMR.strings.backup_uncategorized),
            checked = !state.filterLibraryEntries || state.includeUncategorized,
            onCheckedChange = { model.setIncludeUncategorized(it, categories) },
            enabled = state.options.libraryEntries,
        )
    }

    @Composable
    private fun Options(
        options: ImmutableList<BackupOptions.Entry>,
        state: CreateBackupScreenModel.State,
        model: CreateBackupScreenModel,
    ) {
        options.forEach { option ->
            LabeledCheckbox(
                label = stringResource(option.label),
                checked = option.getter(state.options),
                onCheckedChange = {
                    model.toggle(option.setter, it)
                },
                enabled = option.enabled(state.options),
            )
        }
    }
}

private class CreateBackupScreenModel : StateScreenModel<CreateBackupScreenModel.State>(State()) {

    fun toggle(setter: (BackupOptions, Boolean) -> BackupOptions, enabled: Boolean) {
        mutableState.update {
            it.copy(
                options = setter(it.options, enabled),
            )
        }
    }

    fun createBackup(context: Context, uri: Uri) {
        val state = state.value
        BackupCreateJob.startNow(
            context = context,
            uri = uri,
            options = state.options,
            entryFilter = BackupEntryFilter(
                categoryIds = state.categoryIds,
                includeUncategorized = state.includeUncategorized,
                enabled = state.filterLibraryEntries,
            ),
        )
    }

    fun selectAllCategories(all: Boolean, categories: List<Category>) {
        mutableState.update {
            it.copy(
                filterLibraryEntries = !all,
                categoryIds = if (all) emptySet() else categories.map(Category::id).toSet(),
                includeUncategorized = true,
            )
        }
    }

    fun toggleCategory(categoryId: Long, enabled: Boolean, categories: List<Category>) {
        mutableState.update {
            val selected = if (it.filterLibraryEntries) it.categoryIds else categories.map(Category::id).toSet()
            it.copy(
                filterLibraryEntries = true,
                categoryIds = if (enabled) selected + categoryId else selected - categoryId,
            )
        }
    }

    fun setIncludeUncategorized(enabled: Boolean, categories: List<Category>) {
        mutableState.update {
            it.copy(
                filterLibraryEntries = true,
                categoryIds = if (it.filterLibraryEntries) it.categoryIds else categories.map(Category::id).toSet(),
                includeUncategorized = enabled,
            )
        }
    }

    @Immutable
    data class State(
        val options: BackupOptions = BackupOptions(),
        val filterLibraryEntries: Boolean = false,
        val categoryIds: Set<Long> = emptySet(),
        val includeUncategorized: Boolean = true,
    )
}
