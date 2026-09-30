package tachiyomi.domain.manga.model

/**
 * A relationship supplied by a tracker or a source.  It deliberately does not
 * represent a database row: opening a relation must not create a tracking
 * binding or alter Suggestions.
 */
data class StructuredRelation(
    val title: String,
    val relation: StructuredRelationType,
    val url: String? = null,
    val trackerId: Long? = null,
    val remoteId: Long? = null,
    val coverUrl: String? = null,
    val sourceUrl: String? = null,
    val localAnimeId: Long? = null,
)

enum class StructuredRelationType {
    PREQUEL,
    SEQUEL,
    ADAPTATION,
    ALTERNATIVE,
    SIDE_STORY,
    SPIN_OFF,
    PARENT,
    SUMMARY,
    CAMEO,
    CHARACTER_FOCUS,
    COMPILATION,
    CONTAINS,
    CROSSOVER,
    EXPANSION,
    MAIN,
    MAIN_STORY,
    PARODY,
    REBOOT,
    REMAKE,
    SAME_UNIVERSE,
    SERIES,
    SOURCE,
    UNCOLLECTED,
    DOUJINSHI,
    COLORED,
    MONOCHROME,
    ALTERNATE_STORY,
    ALTERNATE_VERSION,
    PRESERIALIZATION,
    SERIALIZATION,
    OTHER,
    ALTERNATIVE_SETTING,
    ALTERNATIVE_VERSION,
    PARENT_STORY,
    FULL_STORY,
    CHARACTER,
    COMPLATION,
    ;

    companion object {
        fun from(value: String?) = entries.find { it.name.equals(value, ignoreCase = true) } ?: OTHER
    }
}

/** A provider may return no data without preventing the next provider from running. */
fun interface StructuredRelationProvider<T> {
    suspend fun relations(item: T): List<StructuredRelation>
}
