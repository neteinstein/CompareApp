package org.neteinstein.compareapp.utils

import java.math.BigDecimal
import java.math.RoundingMode

object MathUtils {
    fun roundDecimal(value: Double, scale: Int = 6): Double =
        // BigDecimal.valueOf uses the double's canonical decimal string (via Double.toString),
        // rather than its exact binary value (which the BigDecimal(Double) constructor uses and
        // which can be a hair below/above the intended decimal, throwing off HALF_UP rounding).
        BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).toDouble()
}
