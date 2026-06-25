package com.airlock.core

/**
 * Detects and redacts personally identifiable information in free text. Pure Kotlin / regex with
 * real validation (Luhn for cards, mod-97 for IBANs, octet range for IPs) to keep false positives
 * down. No dictionaries, no network.
 */
class PiiDetector {

    private companion object {
        val EMAIL = Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""")
        val IPV4 = Regex("""\b(?:\d{1,3}\.){3}\d{1,3}\b""")
        // 13–19 digits with optional single space/hyphen separators.
        val CARD = Regex("""\b\d(?:[ \-]?\d){12,18}\b""")
        val IBAN = Regex("""\b[A-Z]{2}\d{2}[A-Z0-9]{11,30}\b""")
        // +country, groups, 8–15 digits total.
        val PHONE = Regex("""(?<![\w.])\+?\d[\d\s\-().]{6,}\d(?![\w.])""")
        const val REDACTION = "[REDACTED]"
    }

    /** All PII matches, de-overlapped by priority, sorted by position. */
    fun detect(text: String, types: Set<PiiType> = PiiType.entries.toSet()): List<PiiMatch> {
        val raw = mutableListOf<PiiMatch>()

        if (PiiType.EMAIL in types) EMAIL.findAll(text).forEach {
            raw += PiiMatch(PiiType.EMAIL, it.value, it.range.first, it.range.last + 1)
        }
        if (PiiType.IBAN in types) IBAN.findAll(text).forEach {
            if (isValidIban(it.value)) raw += PiiMatch(PiiType.IBAN, it.value, it.range.first, it.range.last + 1)
        }
        if (PiiType.CREDIT_CARD in types) CARD.findAll(text).forEach {
            val digits = it.value.filter(Char::isDigit)
            if (digits.length in 13..19 && luhnValid(digits)) {
                raw += PiiMatch(PiiType.CREDIT_CARD, it.value, it.range.first, it.range.last + 1)
            }
        }
        if (PiiType.IPV4 in types) IPV4.findAll(text).forEach {
            if (it.value.split('.').all { o -> o.toIntOrNull()?.let { n -> n in 0..255 } == true }) {
                raw += PiiMatch(PiiType.IPV4, it.value, it.range.first, it.range.last + 1)
            }
        }
        if (PiiType.PHONE in types) PHONE.findAll(text).forEach {
            val digits = it.value.filter(Char::isDigit)
            if (digits.length in 8..15) {
                raw += PiiMatch(PiiType.PHONE, it.value.trim(), it.range.first, it.range.last + 1)
            }
        }

        return deOverlap(raw)
    }

    /** Returns redacted text plus the matches that were redacted (positions refer to the input). */
    fun redactText(text: String, types: Set<PiiType>): Pair<String, List<PiiMatch>> {
        val matches = detect(text, types)
        if (matches.isEmpty()) return text to emptyList()
        val sb = StringBuilder(text)
        // Apply from the end so earlier indices stay valid.
        for (m in matches.sortedByDescending { it.start }) {
            sb.replace(m.start, m.end, REDACTION)
        }
        return sb.toString() to matches
    }

    // --- validation helpers ----------------------------------------------------

    /** Priority when two matches overlap: card > iban > email > phone > ipv4. */
    private fun deOverlap(matches: List<PiiMatch>): List<PiiMatch> {
        val priority = mapOf(
            PiiType.CREDIT_CARD to 0, PiiType.IBAN to 1, PiiType.EMAIL to 2,
            PiiType.PHONE to 3, PiiType.IPV4 to 4,
        )
        val sorted = matches.sortedWith(
            compareBy({ priority[it.type] }, { -(it.end - it.start) })
        )
        val kept = mutableListOf<PiiMatch>()
        for (m in sorted) {
            if (kept.none { it.start < m.end && m.start < it.end }) kept += m
        }
        return kept.sortedBy { it.start }
    }

    private fun luhnValid(digits: String): Boolean {
        var sum = 0
        var alt = false
        for (i in digits.indices.reversed()) {
            var d = digits[i] - '0'
            if (alt) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
            alt = !alt
        }
        return sum % 10 == 0
    }

    private fun isValidIban(iban: String): Boolean {
        val s = iban.uppercase().filter { it.isLetterOrDigit() }
        if (s.length < 15 || s.length > 34) return false
        val rearranged = s.substring(4) + s.substring(0, 4)
        val numeric = buildString {
            for (c in rearranged) {
                if (c.isDigit()) append(c) else append((c - 'A' + 10).toString())
            }
        }
        // mod-97 over a long numeric string, chunked to avoid overflow.
        var remainder = 0
        for (c in numeric) {
            remainder = (remainder * 10 + (c - '0')) % 97
        }
        return remainder == 1
    }
}
