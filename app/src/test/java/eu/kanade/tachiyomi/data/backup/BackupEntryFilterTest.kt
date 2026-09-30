package eu.kanade.tachiyomi.data.backup

import eu.kanade.tachiyomi.data.backup.create.BackupEntryFilter
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BackupEntryFilterTest {
    @Test
    fun `disabled filter includes every library entry`() {
        BackupEntryFilter(enabled = false).includes(listOf(99)) shouldBe true
    }

    @Test
    fun `selected categories and uncategorized are independently honored`() {
        val filter = BackupEntryFilter(categoryIds = setOf(2), includeUncategorized = true, enabled = true)

        filter.includes(listOf(1, 2)) shouldBe true
        filter.includes(listOf(1)) shouldBe false
        filter.includes(emptyList()) shouldBe true
        filter.copy(includeUncategorized = false).includes(emptyList()) shouldBe false
    }
}
