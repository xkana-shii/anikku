package eu.kanade.tachiyomi.data.track.anilist

import eu.kanade.tachiyomi.data.track.TrackerManager
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import tachiyomi.domain.manga.model.StructuredRelation
import tachiyomi.domain.manga.model.StructuredRelationType

internal fun JsonObject.toStructuredRelations(): List<StructuredRelation> =
    ((this["data"] as? JsonObject)?.get("Media") as? JsonObject)?.get("relations")
        ?.let { it as? JsonObject }
        ?.get("edges")
        ?.jsonArray
        .orEmpty()
        .mapNotNull { element ->
            val edge = element.jsonObject
            val node = edge["node"] as? JsonObject ?: return@mapNotNull null
            if (node["type"]?.jsonPrimitive?.contentOrNull != "ANIME") return@mapNotNull null
            val remoteId = node["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val titles = node["title"] as? JsonObject ?: return@mapNotNull null
            val title = listOf("userPreferred", "english", "romaji").firstNotNullOfOrNull {
                titles[it]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
            } ?: return@mapNotNull null
            StructuredRelation(
                title = title,
                relation = StructuredRelationType.from(edge["relationType"]?.jsonPrimitive?.contentOrNull),
                url = node["siteUrl"]?.jsonPrimitive?.contentOrNull ?: AnilistApi.animeUrl(remoteId),
                trackerId = TrackerManager.ANILIST,
                remoteId = remoteId,
                coverUrl = (node["coverImage"] as? JsonObject)?.get("large")?.jsonPrimitive?.contentOrNull,
            )
        }
        .distinctBy { Triple(it.trackerId, it.remoteId, it.relation) }
