package com.airlock.core

/**
 * Tracker / redirect rule set. Curated from the ClearURLs and Neat-URL projects, kept small and
 * offline. Param matching is case-insensitive. Everything here is data, not behavior — so the rules
 * can be tested, extended in Settings, and exported/imported as JSON.
 */
object ScrubRules {

    /** Exact parameter names to strip, regardless of value. */
    val EXACT_PARAMS: Set<String> = setOf(
        // Google / Ads click ids
        "gclid", "gclsrc", "gbraid", "wbraid", "dclid", "gad_source", "wickedid",
        // Meta / Facebook / Instagram
        "fbclid", "igshid", "igsh", "mibextid",
        // Microsoft / Bing
        "msclkid",
        // X / Twitter
        "twclid",
        // TikTok
        "ttclid", "tt_medium", "tt_content",
        // LinkedIn
        "li_fat_id",
        // Yandex / Mail.ru
        "yclid", "ysclid", "_openstat",
        // Google Analytics cross-domain
        "_ga", "_gl",
        // Mailchimp / HubSpot / Marketo / Vero / Oracle
        "mc_cid", "mc_eid", "_hsenc", "_hsmi", "hsctatracking", "mkt_tok",
        "vero_id", "vero_conv", "oly_anon_id", "oly_enc_id",
        // Common share / referral tokens
        "ref", "ref_src", "ref_url", "referrer", "spm", "scm", "share_id",
        "s_cid", "cmpid", "campaign_id", "ncid", "rb_clickid", "guccounter",
        "ga_source", "ga_medium", "ga_campaign", "ga_content", "ga_term",
    )

    /** Any parameter whose name starts with one of these prefixes is stripped. */
    val PREFIX_PARAMS: List<String> = listOf(
        "utm_",      // utm_source, utm_medium, utm_campaign, utm_id, utm_marketing_tactic, ...
        "pk_",       // Matomo / Piwik
        "piwik_",
        "matomo_",
        "mtm_",      // Matomo new style
        "stm_",
        "hsa_",      // HubSpot ads
        "__hs",
        "__s",
    )

    /**
     * Redirect wrappers that encode the *real* destination in a query parameter — so we can unwrap
     * them OFFLINE. We deliberately exclude opaque shorteners (bit.ly, t.co): unwrapping those needs
     * a network request, which Airlock will never make.
     *
     * Key = host (no leading www.); value = the param name holding the encoded destination URL.
     */
    val REDIRECT_HOSTS: Map<String, String> = mapOf(
        "google.com" to "url",          // /url?q= or /url?url=
        "l.facebook.com" to "u",
        "lm.facebook.com" to "u",
        "l.instagram.com" to "u",
        "out.reddit.com" to "url",
        "away.vk.com" to "to",
        "youtube.com" to "q",           // /redirect?q=
        "steamcommunity.com" to "url",  // /linkfilter/?url=
        "href.li" to "",                // href.li/?<target> — target is the raw query string
        "nelreports.net" to "url",
        "deref-gmx.net" to "redirectUrl",
        "deref-web.de" to "redirectUrl",
        "safelinks.protection.outlook.com" to "url",
    )

    /** Alternate destination params some hosts use (checked in order). */
    val REDIRECT_FALLBACK_PARAMS: List<String> = listOf("url", "q", "u", "to", "target", "dest", "redirectUrl")

    fun isTrackingParam(name: String): Boolean {
        val n = name.lowercase()
        if (n in EXACT_PARAMS) return true
        return PREFIX_PARAMS.any { n.startsWith(it) }
    }
}
