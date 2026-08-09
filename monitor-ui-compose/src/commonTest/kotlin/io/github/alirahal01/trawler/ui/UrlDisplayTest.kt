package io.github.alirahal01.trawler.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class UrlDisplayTest {

    @Test
    fun extractsHostFromOrdinaryUrl() {
        assertEquals("example.com", hostOf("https://example.com/v1/users/42"))
    }

    @Test
    fun stripsPortAndUserinfo() {
        assertEquals("example.com", hostOf("https://user:pass@example.com:8443/path"))
    }

    @Test
    fun stripsQueryAndFragment() {
        assertEquals("example.com", hostOf("https://example.com?a=1#frag"))
    }
}
