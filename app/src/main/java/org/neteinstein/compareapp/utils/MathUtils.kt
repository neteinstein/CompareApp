package org.neteinstein.compareapp.utils

import java.math.BigDecimal
import java.math.RoundingMode

object MathUtils {
    fun roundDecimal(value: Double, scale: Int = 6): Double =
        BigDecimal(value).setScale(scale, RoundingMode.HALF_UP).toDouble()
}
