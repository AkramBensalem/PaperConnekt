package com.paperconnekt.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CitationsTest {
    private val known = setOf("c1", "c2", "c7")

    @Test
    fun stripsMarkersAndKeepsPageCitations() {
        val report = Citations.verify("The loss uses a temperature of 0.07 (p. 2, §2 Method) [c2].") { it in known }
        assertEquals("The loss uses a temperature of 0.07 (p. 2, §2 Method).", report.text)
        assertEquals(listOf("c2"), report.cited)
        assertTrue(report.verified)
    }

    @Test
    fun splitsGroupedMarkersAndFlagsUnknownIds() {
        val report = Citations.verify("Claim A [c1, C7]. Claim B [c99; c1].") { it in known }
        assertEquals("Claim A. Claim B.", report.text)
        assertEquals(listOf("c1", "c7"), report.cited)
        assertEquals(listOf("c99"), report.unknown)
        assertFalse(report.verified)
    }

    @Test
    fun leavesCodeBlocksAndOrdinaryBracketsUntouched() {
        val text = "See [the appendix] and:\n```\nvalue = a[c1]\n```"
        val report = Citations.verify(text) { it in known }
        assertEquals(text, report.text)
        assertTrue(report.cited.isEmpty())
    }
}
