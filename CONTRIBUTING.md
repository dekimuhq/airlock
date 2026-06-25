# Contributing to Airlock

Thanks for helping make sharing safer. Airlock is small on purpose — keep contributions
focused and well-tested.

## Ground rules

1. **Never add `android.permission.INTERNET`** (or any networking dependency). The whole
   value proposition is that Airlock *cannot* reach the network. A PR that adds network
   access will be closed.
2. **No database, no analytics, no telemetry.** Settings stay in DataStore; nothing the user
   scrubs is persisted.
3. **No proprietary / Play Services / ML-blob dependencies.** Pure-OSS only.
4. **Don't overclaim.** If a feature is best-effort, say so (e.g. we cannot unwrap opaque
   shorteners offline — and we don't pretend to).

## Where things go

- Pure logic (rules, URL/PII handling) → `airlock-core`, with JUnit tests. This is the
  default home for new behavior because it's fast to test.
- Anything that touches `Bitmap`, `Uri`, EXIF, or Compose → `app`.

## Before opening a PR

```bash
./gradlew :airlock-core:test          # logic tests must pass
./gradlew :app:assembleDebug          # must compile
./gradlew :app:connectedDebugAndroidTest   # if you touched image scrubbing
```

Verify the guarantee still holds:

```bash
aapt2 dump permissions app/build/outputs/apk/debug/app-debug.apk | grep -i internet
# must print nothing
```

## Good first issues

- Add more redirect-wrapper hosts to `ScrubRules.REDIRECT_HOSTS` (must be offline-decodable).
- Expand the tracker param list from upstream ClearURLs rules.
- PDF metadata stripping (pure-Kotlin, no new heavy deps).
- Localization (the codebase is English-only today).
