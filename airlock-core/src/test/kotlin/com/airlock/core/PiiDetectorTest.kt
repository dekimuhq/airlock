package com.airlock.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class PiiDetectorTest {
    private val d = PiiDetector()

    @Test fun detectsEmail() {
        val m = d.detect("contact me at jane.doe@example.co.uk please", setOf(PiiType.EMAIL))
        assertEquals(1, m.size)
        assertEquals(PiiType.EMAIL, m[0].type)
        assertEquals("jane.doe@example.co.uk", m[0].value)
    }

    @Test fun detectsValidCreditCardWithLuhn() {
        // 4242 4242 4242 4242 is a Luhn-valid test number.
        val m = d.detect("card: 4242 4242 4242 4242", setOf(PiiType.CREDIT_CARD))
        assertEquals(1, m.size)
        assertEquals(PiiType.CREDIT_CARD, m[0].type)
    }

    @Test fun rejectsLuhnInvalidCard() {
        val m = d.detect("number 1234 5678 9012 3456", setOf(PiiType.CREDIT_CARD))
        assertTrue(m.isEmpty())
    }

    @Test fun detectsValidIpv4Only() {
        val m = d.detect("server 192.168.1.10 and bogus 999.1.1.1", setOf(PiiType.IPV4))
        assertEquals(1, m.size)
        assertEquals("192.168.1.10", m[0].value)
    }

    @Test fun detectsPhone() {
        val m = d.detect("call +34 612 345 678 today", setOf(PiiType.PHONE))
        assertEquals(1, m.size)
        assertEquals(PiiType.PHONE, m[0].type)
    }

    @Test fun validatesIbanMod97() {
        // Valid sample IBAN (mod-97 == 1).
        val good = d.detect("IBAN GB82WEST12345698765432", setOf(PiiType.IBAN))
        assertEquals(1, good.size)
        val bad = d.detect("IBAN GB00WEST12345698765432", setOf(PiiType.IBAN))
        assertTrue(bad.isEmpty())
    }

    @Test fun redactsReplacesValuesAndPreservesSurroundingText() {
        val (out, matches) = d.redactText(
            "Email jane@example.com or call +34 612 345 678",
            setOf(PiiType.EMAIL, PiiType.PHONE),
        )
        assertFalse(out.contains("jane@example.com"))
        assertFalse(out.contains("612 345 678"))
        assertTrue(out.startsWith("Email [REDACTED]"))
        assertEquals(2, matches.size)
    }

    @Test fun respectsTypeFilter() {
        val m = d.detect("jane@example.com 192.168.0.1", setOf(PiiType.EMAIL))
        assertEquals(1, m.size)
        assertEquals(PiiType.EMAIL, m[0].type)
    }

    @Test fun deOverlapsCardOverPhone() {
        // A 16-digit card also matches the phone pattern; card must win, only one match kept.
        val m = d.detect("4242 4242 4242 4242", setOf(PiiType.CREDIT_CARD, PiiType.PHONE))
        assertEquals(1, m.size)
        assertEquals(PiiType.CREDIT_CARD, m[0].type)
    }
}
