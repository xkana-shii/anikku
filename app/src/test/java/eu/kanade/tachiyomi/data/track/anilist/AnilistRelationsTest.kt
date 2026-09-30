package eu.kanade.tachiyomi.data.track.anilist

import eu.kanade.tachiyomi.data.track.TrackerManager
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.StructuredRelationType

class AnilistRelationsTest {
    @Test
    fun `normalizes anime relations and ignores non-anime nodes`() {
        val payload = Json.parseToJsonElement(
            """
            {
              "data": {
                "Media": {
                  "relations": {
                    "edges": [
                      {
                        "relationType": "PREQUEL",
                        "node": {
                          "id": 42,
                          "type": "ANIME",
                          "title": { "userPreferred": "Earlier Show" },
                          "siteUrl": "https://anilist.co/anime/42",
                          "coverImage": { "large": "https://example.test/42.jpg" }
                        }
                      },
                      {
                        "relationType": "SOURCE",
                        "node": {
                          "id": 7,
                          "type": "MANGA",
                          "title": { "userPreferred": "Original Manga" }
                        }
                      }
                    ]
                  }
                }
              }
            }
            """.trimIndent(),
        ).jsonObject

        val relations = payload.toStructuredRelations()

        relations shouldHaveSize 1
        relations.single().title shouldBe "Earlier Show"
        relations.single().relation shouldBe StructuredRelationType.PREQUEL
        relations.single().trackerId shouldBe TrackerManager.ANILIST
        relations.single().remoteId shouldBe 42
    }
}
