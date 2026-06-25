package com.airlock.core

import java.net.URLDecoder

/**
 * Removes tracking parameters from URLs and unwraps offline-decodable redirect wrappers.
 *
 * Pure Kotlin, no network, no Android. URL parsing is done by hand (not [java.net.URI]) so that
 * messy real-world links never throw — we degrade gracefully instead.
 */
class LinkScrubber(
    private val extraExactParams: Set<String> = emptySet(),
) {
    private companion object {
        const val MAX_UNWRAP_DEPTH = 5
        val TRAILING_PUNCT = ".,;:!?)]}>\"'".toSet()
        // http(s) URLs; stop at whitespace and a few delimiters.
        val URL_REGEX = Regex("""https?://[^\s<>"')\]}]+""", RegexOption.IGNORE_CASE)
    }

    private fun isTracking(name: String): Boolean =
        ScrubRules.isTrackingParam(name) || name.lowercase() in extraExactParams

    /** Clean a single URL string. Non-URLs are returned unchanged. */
    fun cleanUrl(raw: String): UrlCleanResult {
        val removed = mutableListOf<String>()
        var unwrapped = false
        var current = raw.trim()

        var depth = 0
        while (depth < MAX_UNWRAP_DEPTH) {
            val parts = parse(current) ?: break
            val dest = extractRedirectDestination(parts)
            if (dest != null && dest != current && parse(dest) != null) {
                current = dest
                unwrapped = true
                depth++
                continue
            }
            break
        }

        val parts = parse(current) ?: return UrlCleanResult(raw, raw, emptyList(), unwrapped)
        val keptQuery = parts.query.filter { (name, value) ->
            if (isTracking(name)) {
                removed.add(if (value.isEmpty()) name else "$name=$value")
                false
            } else true
        }
        val keptFragment = cleanFragment(parts.fragment, removed)

        val cleaned = rebuild(parts.copy(query = keptQuery, fragment = keptFragment))
        return UrlCleanResult(raw, cleaned, removed, unwrapped)
    }

    /** Find every http(s) URL in [text], clean each in place, and (optionally) redact PII. */
    fun cleanText(
        text: String,
        redactPii: Boolean = false,
        piiTypes: Set<PiiType> = emptySet(),
        piiDetector: PiiDetector = PiiDetector(),
    ): TextCleanResult {
        val removed = mutableListOf<String>()
        var urlsProcessed = 0
        var unwrappedCount = 0

        val withCleanLinks = URL_REGEX.replace(text) { m ->
            val match = m.value
            // Peel trailing punctuation that the regex greedily swallowed.
            var end = match.length
            while (end > 0 && match[end - 1] in TRAILING_PUNCT) end--
            val core = match.substring(0, end)
            val suffix = match.substring(end)

            val res = cleanUrl(core)
            urlsProcessed++
            removed.addAll(res.removedParams)
            if (res.unwrapped) unwrappedCount++
            res.cleaned + suffix
        }

        var finalText = withCleanLinks
        val redacted = mutableListOf<PiiMatch>()
        if (redactPii && piiTypes.isNotEmpty()) {
            val r = piiDetector.redactText(finalText, piiTypes)
            finalText = r.first
            redacted.addAll(r.second)
        }

        return TextCleanResult(text, finalText, urlsProcessed, removed, unwrappedCount, redacted)
    }

    // --- URL parsing internals -------------------------------------------------

    private data class Parsed(
        val scheme: String,
        val authority: String,
        val path: String,
        val query: List<Pair<String, String>>, // ordered, raw (encoded) name/value
        val fragment: String,
    ) {
        val host: String
            get() = authority.substringAfterLast('@').substringBefore(':').lowercase()
    }

    private fun parse(raw: String): Parsed? {
        val schemeSep = raw.indexOf("://")
        if (schemeSep <= 0) return null
        val scheme = raw.substring(0, schemeSep).lowercase()
        if (scheme != "http" && scheme != "https") return null
        var rest = raw.substring(schemeSep + 3)

        var fragment = ""
        val hashIdx = rest.indexOf('#')
        if (hashIdx >= 0) {
            fragment = rest.substring(hashIdx + 1)
            rest = rest.substring(0, hashIdx)
        }

        var queryStr = ""
        val qIdx = rest.indexOf('?')
        if (qIdx >= 0) {
            queryStr = rest.substring(qIdx + 1)
            rest = rest.substring(0, qIdx)
        }

        val pathIdx = rest.indexOfFirst { it == '/' }
        val authority: String
        val path: String
        if (pathIdx >= 0) {
            authority = rest.substring(0, pathIdx)
            path = rest.substring(pathIdx)
        } else {
            authority = rest
            path = ""
        }
        if (authority.isEmpty()) return null

        val query = parseQuery(queryStr)
        return Parsed(scheme, authority, path, query, fragment)
    }

    private fun parseQuery(q: String): List<Pair<String, String>> {
        if (q.isEmpty()) return emptyList()
        return q.split('&').filter { it.isNotEmpty() }.map { pair ->
            val eq = pair.indexOf('=')
            if (eq >= 0) pair.substring(0, eq) to pair.substring(eq + 1)
            else pair to ""
        }
    }

    private fun extractRedirectDestination(p: Parsed): String? {
        val host = p.host.removePrefix("www.")
        // href.li/?https://target  — destination is the raw query string itself.
        if (host == "href.li") {
            val rawQ = p.query.joinToString("&") { (n, v) -> if (v.isEmpty()) n else "$n=$v" }
            return decode(rawQ).ifEmpty { null }
        }
        val param = ScrubRules.REDIRECT_HOSTS[host] ?: ScrubRules.REDIRECT_HOSTS[p.host]
        val candidates = buildList {
            if (param != null && param.isNotEmpty()) add(param)
            addAll(ScrubRules.REDIRECT_FALLBACK_PARAMS)
        }
        if (param == null) return null // host not a known wrapper
        for (key in candidates) {
            val hit = p.query.firstOrNull { it.first.equals(key, ignoreCase = true) } ?: continue
            val decoded = decode(hit.second)
            if (decoded.startsWith("http://") || decoded.startsWith("https://")) return decoded
        }
        return null
    }

    private fun cleanFragment(fragment: String, removed: MutableList<String>): String {
        if (fragment.isEmpty() || !fragment.contains('=')) return fragment
        // Only treat the fragment as params if it looks like key=value(&key=value)* — leaves SPA
        // routes like #/settings untouched.
        val looksLikeParams = fragment.split('&').all { it.contains('=') && !it.contains('/') }
        if (!looksLikeParams) return fragment
        return parseQuery(fragment).filter { (name, value) ->
            if (isTracking(name)) {
                removed.add(if (value.isEmpty()) name else "$name=$value")
                false
            } else true
        }.joinToString("&") { (n, v) -> if (v.isEmpty()) n else "$n=$v" }
    }

    private fun rebuild(p: Parsed): String {
        val sb = StringBuilder()
        sb.append(p.scheme).append("://").append(p.authority).append(p.path)
        if (p.query.isNotEmpty()) {
            sb.append('?').append(p.query.joinToString("&") { (n, v) -> if (v.isEmpty()) n else "$n=$v" })
        }
        if (p.fragment.isNotEmpty()) sb.append('#').append(p.fragment)
        return sb.toString()
    }

    private fun decode(s: String): String = try {
        URLDecoder.decode(s, "UTF-8")
    } catch (_: Exception) {
        s
    }
}
