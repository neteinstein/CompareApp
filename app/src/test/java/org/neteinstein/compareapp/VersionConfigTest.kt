package org.neteinstein.compareapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for version configuration.
 * Validates that the version follows the MAJOR.MINOR.PATCH format.
 */
class VersionConfigTest {

    companion object {
        // Semantic versioning pattern: MAJOR.MINOR.PATCH where each part is a number
        private val SEMANTIC_VERSION_PATTERN = Regex("""^\d+\.\d+\.\d+$""")
    }

    @Test
    fun testVersionName_followsSemanticVersioning() {
        // Given
        val versionName = BuildConfig.VERSION_NAME

        // Then - Should match semantic versioning pattern (e.g., "1.0.0")
        assertTrue(
            "Version name should follow MAJOR.MINOR.PATCH format (e.g., 1.0.0)",
            SEMANTIC_VERSION_PATTERN.matches(versionName)
        )
    }

    @Test
    fun testVersionCode_isPositiveInteger() {
        // Given
        val versionCode = BuildConfig.VERSION_CODE

        // Then - Version code should be a positive integer
        assertTrue("Version code should be positive", versionCode > 0)
    }

    @Test
    fun testVersionName_hasThreeParts() {
        // Given
        val versionName = BuildConfig.VERSION_NAME

        // Then - Version should have exactly 3 parts (MAJOR.MINOR.PATCH)
        val parts = versionName.split(".")
        assertEquals("Version should have exactly 3 parts (MAJOR.MINOR.PATCH)", 3, parts.size)
        
        // Verify each part is a valid number
        parts.forEach { part ->
            assertTrue("Each version part should be a number: $part", part.toIntOrNull() != null)
        }
    }
}
