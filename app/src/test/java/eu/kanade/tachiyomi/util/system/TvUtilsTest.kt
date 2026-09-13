package eu.kanade.tachiyomi.util.system

import android.content.res.Configuration
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TvUtilsTest {

    @Test
    fun `television ui mode enables remote navigation`() {
        assertTrue(isTelevision(Configuration.UI_MODE_TYPE_TELEVISION, hasLeanbackFeature = false))
    }

    @Test
    fun `leanback feature enables remote navigation`() {
        assertTrue(isTelevision(Configuration.UI_MODE_TYPE_NORMAL, hasLeanbackFeature = true))
    }

    @Test
    fun `ordinary devices do not enable remote navigation`() {
        assertFalse(isTelevision(Configuration.UI_MODE_TYPE_NORMAL, hasLeanbackFeature = false))
    }
}
