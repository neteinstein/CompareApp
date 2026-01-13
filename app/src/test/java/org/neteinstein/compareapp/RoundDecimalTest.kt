package org.neteinstein.compareapp

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoundDecimalTest {

    private lateinit var activity: MainActivity

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .get()
    }

    @Test
    fun testRoundDecimal_withDefaultScale() {
        // Given
        val value = 12.3456789

        // When
        val result = activity.roundDecimal(value)

        // Then
        assertEquals(12.345679, result, 0.000001)
    }

    @Test
    fun testRoundDecimal_withCustomScale() {
        // Given
        val value = 40.7589648

        // When
        val result = activity.roundDecimal(value, 6)

        // Then
        assertEquals(40.758965, result, 0.000001)
    }

    @Test
    fun testRoundDecimal_withLowerScale() {
        // Given
        val value = 73.9851301

        // When
        val result = activity.roundDecimal(value, 2)

        // Then
        assertEquals(73.99, result, 0.01)
    }

    @Test
    fun testRoundDecimal_withZeroScale() {
        // Given
        val value = 12.56789

        // When
        val result = activity.roundDecimal(value, 0)

        // Then
        assertEquals(13.0, result, 0.01)
    }

    @Test
    fun testRoundDecimal_withNegativeNumber() {
        // Given
        val value = -40.7589648

        // When
        val result = activity.roundDecimal(value, 6)

        // Then
        assertEquals(-40.758965, result, 0.000001)
    }

    @Test
    fun testRoundDecimal_withZero() {
        // Given
        val value = 0.0

        // When
        val result = activity.roundDecimal(value, 6)

        // Then
        assertEquals(0.0, result, 0.000001)
    }

    @Test
    fun testRoundDecimal_withRoundingUp() {
        // Given
        val value = 40.7589655

        // When
        val result = activity.roundDecimal(value, 6)

        // Then
        assertEquals(40.758966, result, 0.000001)
    }

    @Test
    fun testRoundDecimal_withRoundingDown() {
        // Given
        val value = 40.7589644

        // When
        val result = activity.roundDecimal(value, 6)

        // Then
        assertEquals(40.758964, result, 0.000001)
    }

    @Test
    fun testRoundDecimal_withExactMidpoint() {
        // Given - exactly at midpoint (5)
        val value = 40.7589645

        // When
        val result = activity.roundDecimal(value, 6)

        // Then
        // HALF_UP should round up
        assertEquals(40.758965, result, 0.000001)
    }
}
