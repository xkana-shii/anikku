package eu.kanade.tachiyomi.util.system

import eu.kanade.domain.ui.model.TvUiMode
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TvUiPolicyTest {

    @Test
    fun `automatic follows raw television detection`() {
        assertTrue(resolveTvUiEnabled(TvUiMode.AUTOMATIC, isTelevision = true))
        assertFalse(resolveTvUiEnabled(TvUiMode.AUTOMATIC, isTelevision = false))
    }

    @Test
    fun `always enables television UI without television capability`() {
        assertTrue(resolveTvUiEnabled(TvUiMode.ALWAYS, isTelevision = false))
    }

    @Test
    fun `never disables television UI on a television`() {
        assertFalse(resolveTvUiEnabled(TvUiMode.NEVER, isTelevision = true))
    }
}
