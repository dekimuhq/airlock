package com.airlock.core

/** Result of cleaning a single URL. */
data class UrlCleanResult(
    val original: String,
    val cleaned: String,
    /** Tracking parameters that were removed (name=value form, lowercased name). */
    val removedParams: List<String>,
    /** True if a redirect wrapper was unwrapped to its encoded destination. */
    val unwrapped: Boolean,
) {
    val changed: Boolean get() = original != cleaned
}

/** Result of cleaning free text that may contain URLs and (optionally) PII. */
data class TextCleanResult(
    val original: String,
    val cleaned: String,
    val urlsProcessed: Int,
    val removedParams: List<String>,
    val unwrappedCount: Int,
    val redactedPii: List<PiiMatch>,
) {
    val changed: Boolean get() = original != cleaned
}

enum class PiiType(val label: String) {
    EMAIL("Email"),
    PHONE("Phone"),
    CREDIT_CARD("Card number"),
    IPV4("IP address"),
    IBAN("IBAN"),
}

/** A detected piece of personally identifiable information inside text. */
data class PiiMatch(
    val type: PiiType,
    val value: String,
    val start: Int,
    val end: Int,
)
