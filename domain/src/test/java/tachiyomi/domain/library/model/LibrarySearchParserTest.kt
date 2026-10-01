package tachiyomi.domain.library.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LibrarySearchParserTest {
    @Test
    fun `scoped quoted and free text terms combine`() {
        LibrarySearchParser.parse("title:\"Example Anime\" status:completed extra") shouldBe listOf(
            LibrarySearchToken("Example Anime", "title"),
            LibrarySearchToken("completed", "status"),
            LibrarySearchToken("extra"),
        )
    }

    @Test
    fun `aliases exclusions exact and numeric ids work`() {
        LibrarySearchParser.parse("src:123Anime tags:action -genre:horror cat:\"Currently Watching\" id:12345") shouldBe listOf(
            LibrarySearchToken("123Anime", "source"),
            LibrarySearchToken("action", "tag"),
            LibrarySearchToken("horror", "genre", excluded = true),
            LibrarySearchToken("Currently Watching", "category"),
            LibrarySearchToken("12345", "id"),
        )
        LibrarySearchParser.parse("title:$" + "Example") shouldBe listOf(
            LibrarySearchToken("Example", "title", exact = true),
        )
        LibrarySearchToken("123", "id").matches(listOf("1234")) shouldBe false
    }
}
