package eu.kanade.tachiyomi.ui.browse.source.browse

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BrowseSourceTvFocusTest {

    @Test
    fun `TV listing routes down when a real result exists`() {
        assertTrue(canRouteSourceListingToContent(isTvUi = true, hasLoadedItem = true))
    }

    @Test
    fun `TV listing does not route down during empty or loading state`() {
        assertFalse(canRouteSourceListingToContent(isTvUi = true, hasLoadedItem = false))
    }

    @Test
    fun `non-TV listing keeps native focus behavior`() {
        assertFalse(canRouteSourceListingToContent(isTvUi = false, hasLoadedItem = true))
    }
}
