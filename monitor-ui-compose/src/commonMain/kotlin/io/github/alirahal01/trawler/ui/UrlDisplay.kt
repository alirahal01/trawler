package io.github.alirahal01.trawler.ui

/**
 * Best-effort host extraction for list/detail display, without a URL
 * parsing dependency. Not IPv6-literal-aware — good enough for a debug
 * viewer's list row.
 */
internal fun hostOf(url: String): String {
    val afterScheme = url.substringAfter("://", url)
    val authority = afterScheme.substringBefore("/").substringBefore("?").substringBefore("#")
    val hostAndPort = authority.substringAfter("@")
    return hostAndPort.substringBeforeLast(":")
}
