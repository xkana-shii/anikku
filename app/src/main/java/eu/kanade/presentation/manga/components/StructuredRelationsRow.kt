package eu.kanade.presentation.manga.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.manga.interactor.GetStructuredRelations
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.util.system.openInBrowser
import kotlinx.coroutines.CancellationException
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.StructuredRelation
import tachiyomi.domain.manga.model.StructuredRelationType
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.i18n.MR
import tachiyomi.i18n.ank.AMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.tvFocusable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun StructuredRelationsRow(anime: Manga, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    val trackerManager = remember { Injekt.get<TrackerManager>() }
    val trackPreferences = remember { Injekt.get<TrackPreferences>() }
    val tracks by remember(anime.id) { Injekt.get<GetTracks>().subscribe(anime.id) }.collectAsState(null)
    val preferred by remember { trackPreferences.preferredTrackerForAnime().changes() }.collectAsState("")
    val priority by remember { trackPreferences.priorityTrackerId().changes() }
        .collectAsState(trackPreferences.priorityTrackerId().get())
    val loggedInTrackers by remember { trackerManager.loggedInTrackersFlow() }.collectAsState(emptyList())
    val library by remember { Injekt.get<MangaRepository>().getLibraryMangaAsFlow() }.collectAsState(emptyList())
    val libraryIds = remember(library) { library.map { it.manga.id }.toSet() }
    val bindings = tracks.orEmpty()
    val canLoad = bindings.any { track ->
        trackerManager.get(track.trackerId)?.let { it.isLoggedIn && it.supportsStructuredRelations } == true
    }
    var refresh by remember(anime.id) { mutableIntStateOf(0) }
    var state by remember(anime.id) { mutableStateOf<RelationsState>(RelationsState.Loading) }
    var selected by remember(anime.id) { mutableStateOf<StructuredRelation?>(null) }

    LaunchedEffect(
        anime.id,
        bindings.map { it.trackerId to it.remoteId },
        preferred,
        priority,
        loggedInTrackers.map { it.id },
        refresh,
    ) {
        if (!canLoad) return@LaunchedEffect
        state = RelationsState.Loading
        state = try {
            val entries = withIOContext {
                Injekt.get<GetStructuredRelations>().await(anime, bindings, refresh = refresh > 0)
            }
            if (entries.isEmpty()) RelationsState.Empty else RelationsState.Content(entries)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            RelationsState.Error(e)
        }
    }

    AnimatedVisibility(
        visible = canLoad && state != RelationsState.Empty,
        modifier = modifier,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Column {
            Text(
                text = stringResource(AMR.strings.structured_relations_title),
                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                style = MaterialTheme.typography.titleMedium,
            )
            when (val current = state) {
                RelationsState.Loading -> Row(
                    modifier = Modifier.fillMaxWidth().padding(MaterialTheme.padding.medium),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
                RelationsState.Empty -> Unit
                is RelationsState.Error -> Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.padding.medium),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(AMR.strings.structured_relations_error),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = { refresh++ }) {
                        Text(stringResource(MR.strings.action_retry))
                    }
                }
                is RelationsState.Content -> LazyRow(
                    contentPadding = PaddingValues(MaterialTheme.padding.small),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                ) {
                    items(current.entries, key = { "${it.trackerId}-${it.remoteId}-${it.relation}" }) { entry ->
                        StructuredRelationCard(entry, entry.localAnimeId in libraryIds) {
                            entry.localAnimeId?.let { navigator.push(MangaScreen(it)) } ?: run { selected = entry }
                        }
                    }
                }
            }
        }
    }

    selected?.let { entry ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(entry.title) },
            text = { Text(stringResource(AMR.strings.structured_relations_find_source)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        selected = null
                        navigator.push(GlobalSearchScreen(entry.title))
                    },
                ) { Text(stringResource(MR.strings.action_search)) }
            },
            dismissButton = entry.url?.let { url ->
                @Composable {
                    TextButton(
                        onClick = {
                            selected = null
                            context.openInBrowser(url)
                        },
                    ) { Text(stringResource(MR.strings.action_open_in_browser)) }
                }
            },
        )
    }
}

@Composable
private fun StructuredRelationCard(entry: StructuredRelation, inLibrary: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .width(112.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .tvFocusable(interactionSource)
            .padding(vertical = MaterialTheme.padding.extraSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        Text(
            text = stringResource(entry.relation.label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        entry.coverUrl?.let {
            MangaCover.Book(data = it, modifier = Modifier.width(96.dp), contentDescription = entry.title)
        }
        Text(
            text = entry.title,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (inLibrary) {
            Text(
                text = stringResource(MR.strings.in_library),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val StructuredRelationType.label: StringResource
    get() = when (this) {
        StructuredRelationType.PREQUEL -> AMR.strings.relation_prequel
        StructuredRelationType.SEQUEL -> AMR.strings.relation_sequel
        StructuredRelationType.ADAPTATION -> AMR.strings.relation_adaptation
        StructuredRelationType.ALTERNATIVE, StructuredRelationType.ALTERNATIVE_SETTING -> AMR.strings.relation_alternative
        StructuredRelationType.SIDE_STORY -> AMR.strings.relation_side_story
        StructuredRelationType.SPIN_OFF -> AMR.strings.relation_spin_off
        StructuredRelationType.PARENT, StructuredRelationType.PARENT_STORY -> AMR.strings.relation_parent
        StructuredRelationType.SUMMARY -> AMR.strings.relation_summary
        StructuredRelationType.CAMEO -> AMR.strings.relation_cameo
        StructuredRelationType.CHARACTER_FOCUS, StructuredRelationType.CHARACTER -> AMR.strings.relation_character_focus
        StructuredRelationType.COMPILATION, StructuredRelationType.COMPLATION -> AMR.strings.relation_compilation
        StructuredRelationType.CONTAINS -> AMR.strings.relation_contains
        StructuredRelationType.CROSSOVER -> AMR.strings.relation_crossover
        StructuredRelationType.EXPANSION -> AMR.strings.relation_expansion
        StructuredRelationType.MAIN -> AMR.strings.relation_main
        StructuredRelationType.MAIN_STORY, StructuredRelationType.FULL_STORY -> AMR.strings.relation_main_story
        StructuredRelationType.PARODY -> AMR.strings.relation_parody
        StructuredRelationType.REBOOT -> AMR.strings.relation_reboot
        StructuredRelationType.REMAKE -> AMR.strings.relation_remake
        StructuredRelationType.SAME_UNIVERSE -> AMR.strings.relation_same_universe
        StructuredRelationType.SERIES -> AMR.strings.relation_series
        StructuredRelationType.SOURCE -> AMR.strings.relation_source
        StructuredRelationType.UNCOLLECTED -> AMR.strings.relation_uncollected
        StructuredRelationType.DOUJINSHI -> AMR.strings.relation_doujinshi
        StructuredRelationType.COLORED -> AMR.strings.relation_colored
        StructuredRelationType.MONOCHROME -> AMR.strings.relation_monochrome
        StructuredRelationType.ALTERNATE_STORY -> AMR.strings.relation_alternate_story
        StructuredRelationType.ALTERNATE_VERSION,
        StructuredRelationType.ALTERNATIVE_VERSION,
        -> AMR.strings.relation_alternate_version
        StructuredRelationType.PRESERIALIZATION -> AMR.strings.relation_preserialization
        StructuredRelationType.SERIALIZATION -> AMR.strings.relation_serialization
        StructuredRelationType.OTHER -> AMR.strings.relation_other
    }

private sealed interface RelationsState {
    data object Loading : RelationsState
    data object Empty : RelationsState
    data class Content(val entries: List<StructuredRelation>) : RelationsState
    data class Error(val cause: Throwable) : RelationsState
}
