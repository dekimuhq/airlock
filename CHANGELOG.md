# Changelog

## v2.1.1

- **Smaller download** — enabled R8 code shrinking + resource shrinking. The release APK drops
  from ~20 MB to ~9 MB. PDFBox is fully kept (verified the build + UI + scrub code paths run
  under R8), so behavior is unchanged.
- **CI** — GitHub Actions run the unit tests, build the APK, and assert the no-INTERNET
  guarantee on every push/PR; a tag-triggered workflow publishes signed releases (when signing
  secrets are configured).
- **F-Droid** — added fastlane metadata + a ready-to-submit build recipe (`docs/fdroid/`).

## v2.1.0

- **PDF metadata stripping** — removes the PDF Info dictionary (Author, Producer, Creator,
  timestamps) and the XMP metadata stream, fully offline via PDFBox-Android. New "Clean a PDF"
  tile, share-target for `application/pdf`, and a metadata report screen.
- **More redirect wrappers** — added DuckDuckGo, Messenger, LinkedIn, Tumblr, Slack, VK,
  affiliate networks (VigLink, Skimlinks, Rakuten/LinkSynergy) and more to the offline
  unwrapper. Still never touches the network for opaque shorteners.

## v2.0.0 — full release

Adds to the MVP:

- **Batch images** — share multiple photos at once (`ACTION_SEND_MULTIPLE`); export all cleaned.
- **PII auto-redaction in text** — emails, phone numbers, Luhn-valid credit cards, IPv4
  addresses, and mod-97-valid IBANs are detected and replaced.
- **Offline redirect unwrapping** — Google, Facebook, Instagram, Reddit, YouTube, Steam,
  Outlook SafeLinks and more, applied recursively. Opaque shorteners are deliberately left
  untouched (no network is ever used).
- **Privacy report** — after every scrub, see exactly what was removed.
- **Custom tracking parameters** — add your own param names to strip.
- **PROCESS_TEXT integration** — clean selected text straight from any app's selection menu.
- **Session stats** on the home screen.

## v1.0.0 — MVP

- Image metadata destruction by full pixel re-encode (EXIF/XMP/IPTC/maker notes), with
  orientation baked in.
- Irreversible manual redaction (pixels destroyed, not an overlay).
- Link de-tracking (UTM, click-ids, analytics tokens).
- Share-target intents for single image and text.
- Settings via DataStore.
- **No `INTERNET` permission** — the core guarantee.
