package io.github.alirahal01.trawler.sample

import kotlin.math.abs
import kotlin.math.round

private fun groupThousands(intPart: String): String =
    intPart.reversed().chunked(3).joinToString(",").reversed()

/** Formats a plain decimal with thousands separators, e.g. "1,234.56". */
fun formatNumber(value: Double, decimals: Int = 2): String {
    val sign = if (value < 0) "-" else ""
    var scale = 1L
    repeat(decimals) { scale *= 10 }
    val scaled = round(abs(value) * scale).toLong()
    val whole = scaled / scale
    val fraction = (scaled % scale).toString().padStart(decimals, '0')
    val grouped = groupThousands(whole.toString())
    return if (decimals == 0) "$sign$grouped" else "$sign$grouped.$fraction"
}

/** Formats a USD amount with thousands separators and two decimal places, e.g. "$1,234.56". */
fun formatUsd(value: Double): String = "$${formatNumber(value)}"

/** Formats a signed percentage to two decimal places, e.g. "+1.23%". */
fun formatPercent(value: Double): String {
    val rounded = round(value * 100) / 100
    val sign = if (rounded > 0) "+" else ""
    return "$sign$rounded%"
}
