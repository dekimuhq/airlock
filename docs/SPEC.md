# Airlock — Product Spec

> Everything you share leaks who you are. Airlock is the airlock between your data and the world.

## 1. Problem

When you share a photo, a screenshot, or a link, you also share things you never meant to:

- **Photos** carry EXIF: exact GPS coordinates (down to a building), capture timestamp, phone make/model, software version.
- **Screenshots** contain visible PII — names, faces, card numbers, addresses — and "blur"/"crop" tools are often *reversible* (see the Acropalypse-class bugs).
- **Links** are stuffed with tracking parameters (`utm_*`, `fbclid`, `gclid`, …) and redirect wrappers that fingerprint and follow you.

Existing tools each solve **one** slice (Scrambled Exif = metadata only; ClearURLs = links only; paid editors = redaction), are scattered, and many request network access — so you have to *trust* them not to phone home.

## 2. The product

A single on-device **share-sheet airlock**. Share anything *into* Airlock; a sanitized copy comes *out*.

Three scrub engines, all 100% offline:

1. **Metadata destruction** — re-encode the image so **all** metadata (EXIF, XMP, IPTC, maker notes) is gone, not just known tags. Orientation is baked into pixels first so the photo still displays correctly.
2. **Irreversible pixel redaction** — paint over regions; the underlying pixels are **destroyed** in the bitmap, then flattened. No recoverable layer, no reversible blur.
3. **Link & text de-tracking** — strip tracking params, unwrap offline-decodable redirect wrappers, and (v2) redact PII patterns (email/phone/card/IP) in free text.

### The headline guarantee

**Airlock declares no `INTERNET` permission.** It is structurally incapable of exfiltrating your data — verifiable in the manifest and in Android's app-info screen. This is the trust wedge no cloud tool can match.

## 3. Constraints (honored)

| Constraint | How |
|---|---|
| Android-first | Native Kotlin + Jetpack Compose; share-target intents are the core UX |
| No database | DataStore Preferences (key-value file) for settings; no Room/SQLite; no content persisted |
| No payments | None |
| No API / offline | No network calls; no `INTERNET` permission. Only offline redirect-unwrapping (target encoded in the URL) |
| Open source | Apache-2.0; no Google Play Services, no ML binary blobs, no proprietary deps |
| Not a typical app | Not "a blur app" — a destructive, no-network share airlock combining 3 engines |

## 4. Users

Anyone who shares photos/screenshots/links and does not want to leak location, identity, or be tracked — activists, journalists, abuse survivors, and ordinary privacy-conscious people who don't want to pay a subscription or trust a cloud.

## 5. Architecture

- **`airlock-core`** — pure-Kotlin JVM module. No Android deps → fast JUnit tests.
  - `ScrubRules` — tracker param set/prefixes + redirect-unwrap host map.
  - `LinkScrubber` — `cleanUrl()`, `cleanText()`.
  - `PiiDetector` — regex + Luhn detectors; `redactText()`.
  - `models` — result data classes.
- **`app`** — Android module (Compose UI, intents, bitmap/EXIF).
  - `MainActivity` — routes launcher vs `ACTION_SEND`/`ACTION_SEND_MULTIPLE`.
  - `scrub/ImageScrubber` — EXIF read, orientation bake, pixel redaction, re-encode to cache via FileProvider.
  - `intent/ShareIntents` — build outgoing `ACTION_SEND`.
  - `data/SettingsRepository` — DataStore.
  - `ui/*` — Home, ImageScrub, TextScrub, Settings screens + theme.

## 6. MVP scope

- Share image in → strip metadata → manual rectangle redaction → export/share clean image.
- Share/paste text or link in → strip tracking params + unwrap offline redirects → copy/share clean.
- "What was removed" report (EXIF tags found; params stripped).
- Settings: toggle tracker families.
- Zero network permission.

## 7. v2 scope

- **Batch images** (`ACTION_SEND_MULTIPLE`).
- **PII auto-redaction in text** — email, phone, credit card (Luhn-validated), IPv4.
- **Redirect unwrapping** for Google/Facebook/Reddit/YouTube/Steam wrappers, recursive.
- **Privacy report** screen summarizing every removal.
- **Custom rules** — add your own params; import/export rules as JSON.
- **Session stats** on home.

## 8. Out of scope (honest)

- Unwrapping network shorteners (`bit.ly`, `t.co`) — needs a request; would break the no-network guarantee. We say so.
- Auto image OCR/face redaction — would need an ML blob (Google) or network; deferred to keep it pure-OSS. Redaction is manual but **irreversible**.
- "Secure delete" claims — flash wear-leveling makes them dishonest; not offered.

## 9. Verification

- `./gradlew :airlock-core:test` — link/PII logic (primary, runs on JVM here).
- `./gradlew :app:assembleDebug` — produces installable APK.
- Manifest grep proves no `INTERNET` permission.
- Re-reading an exported image proves EXIF is gone (instrumented/manual).
