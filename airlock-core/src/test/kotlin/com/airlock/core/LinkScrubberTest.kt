package com.airlock.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LinkScrubberTest {
    private val scrubber = LinkScrubber()

    @Test fun stripsUtmParams() {
        val r = scrubber.cleanUrl("https://shop.example.com/p/42?utm_source=newsletter&utm_medium=email&color=blue")
        assertEquals("https://shop.example.com/p/42?color=blue", r.cleaned)
        assertTrue(r.changed)
        assertTrue(r.removedParams.any { it.startsWith("utm_source") })
        assertTrue(r.removedParams.any { it.startsWith("utm_medium") })
    }

    @Test fun stripsClickIds() {
        val r = scrubber.cleanUrl("https://news.example.com/article?fbclid=ABC123&gclid=XYZ&id=7")
        assertEquals("https://news.example.com/article?id=7", r.cleaned)
    }

    @Test fun dropsQuestionMarkWhenAllParamsRemoved() {
        val r = scrubber.cleanUrl("https://example.com/page?utm_campaign=spring&igshid=zzz")
        assertEquals("https://example.com/page", r.cleaned)
    }

    @Test fun keepsCleanUrlUnchanged() {
        val url = "https://example.com/path?q=hello&page=2"
        val r = scrubber.cleanUrl(url)
        assertEquals(url, r.cleaned)
        assertFalse(r.changed)
    }

    @Test fun preservesFragmentRoute() {
        val r = scrubber.cleanUrl("https://app.example.com/?utm_source=x#/dashboard")
        assertEquals("https://app.example.com/#/dashboard", r.cleaned)
    }

    @Test fun stripsTrackingFromFragmentParams() {
        val r = scrubber.cleanUrl("https://example.com/#utm_source=x&section=2")
        assertEquals("https://example.com/#section=2", r.cleaned)
    }

    @Test fun unwrapsGoogleRedirect() {
        val r = scrubber.cleanUrl("https://www.google.com/url?q=https%3A%2F%2Ftarget.com%2Fpage%3Futm_source%3Dg&sa=D")
        assertTrue(r.unwrapped)
        assertEquals("https://target.com/page", r.cleaned)
    }

    @Test fun unwrapsFacebookRedirect() {
        val r = scrubber.cleanUrl("https://l.facebook.com/l.php?u=https%3A%2F%2Freal.example.org%2Farticle&h=AT0")
        assertTrue(r.unwrapped)
        assertEquals("https://real.example.org/article", r.cleaned)
    }

    @Test fun doesNotUnwrapOpaqueShortener() {
        // bit.ly cannot be unwrapped offline; must be left intact (no network is ever made).
        val r = scrubber.cleanUrl("https://bit.ly/3abcDEF")
        assertFalse(r.unwrapped)
        assertEquals("https://bit.ly/3abcDEF", r.cleaned)
    }

    @Test fun handlesNonUrlGracefully() {
        val r = scrubber.cleanUrl("just some text, not a url")
        assertEquals("just some text, not a url", r.cleaned)
        assertFalse(r.changed)
    }

    @Test fun cleanTextReplacesUrlsInPlaceAndPeelsPunctuation() {
        val text = "Check this out: https://example.com/x?utm_source=tw&id=9. Thanks!"
        val r = scrubber.cleanText(text)
        assertEquals("Check this out: https://example.com/x?id=9. Thanks!", r.cleaned)
        assertEquals(1, r.urlsProcessed)
    }

    @Test fun cleanTextHandlesMultipleUrls() {
        val text = "a https://a.com/?fbclid=1 b https://b.com/?utm_term=z"
        val r = scrubber.cleanText(text)
        assertEquals("a https://a.com/ b https://b.com/", r.cleaned)
        assertEquals(2, r.urlsProcessed)
    }

    @Test fun customExtraParamIsStripped() {
        val s = LinkScrubber(extraExactParams = setOf("affiliate"))
        val r = s.cleanUrl("https://example.com/?affiliate=joe&keep=1")
        assertEquals("https://example.com/?keep=1", r.cleaned)
    }

    @Test fun caseInsensitiveParamMatch() {
        val r = scrubber.cleanUrl("https://example.com/?UTM_Source=x&FBCLID=y&ok=1")
        assertEquals("https://example.com/?ok=1", r.cleaned)
    }
}
