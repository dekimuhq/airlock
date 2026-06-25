# Submitting Airlock to F-Droid

F-Droid builds apps **from source on their own servers and signs them with their own key** — so
no signing secrets are ever shared, which fits Airlock's trust model perfectly. The official
catalog metadata lives in the `fdroiddata` repo on GitLab; submission is a merge request there.

## Why Airlock qualifies

- ✅ Apache-2.0, fully open source.
- ✅ No proprietary dependencies, **no Google Play Services**, no ML blobs.
- ✅ No trackers, no ads, no analytics (clean against F-Droid's anti-features scan).
- ✅ No `INTERNET` permission.
- ✅ Reproducible-friendly: standard Gradle build, versioned by git tag.

## What's already in this repo

- `docs/fdroid/com.airlock.yml` — the ready-to-submit build recipe.
- `fastlane/metadata/android/en-US/` — title, descriptions, changelog, and screenshots that
  F-Droid (and IzzyOnDroid) read automatically.

## Steps

1. Make sure a signed/tagged release exists (`v2.1.1`) and the app builds cleanly from that tag:
   ```bash
   git checkout v2.1.1
   ./gradlew :app:assembleRelease   # builds unsigned when no keystore.properties — exactly what F-Droid does
   ```
2. Fork `https://gitlab.com/fdroid/fdroiddata`.
3. Copy `docs/fdroid/com.airlock.yml` to `metadata/com.airlock.yml` in your fork.
4. Run F-Droid's linter locally (optional but recommended):
   ```bash
   fdroid lint com.airlock
   fdroid build com.airlock:4    # full build test in their buildserver VM
   ```
5. Commit and open a merge request against `fdroiddata`. The F-Droid CI builds it; respond to
   any reviewer notes.

## Faster alternative: IzzyOnDroid

The IzzyOnDroid F-Droid-compatible repo accepts apps directly from GitHub Releases (it reads the
same `fastlane/` metadata). Open an issue at `https://gitlab.com/IzzyOnDroid/repo` requesting
inclusion of `https://github.com/dekimuhq/airlock`. This is usually the quickest route to a
working F-Droid-style listing while the main fdroiddata MR is reviewed.

## On every new release

Bump `CurrentVersion`/`CurrentVersionCode` (or rely on `UpdateCheckMode: Tags` + `AutoUpdateMode:
Version`, which auto-detects new tags), add `fastlane/.../changelogs/<versionCode>.txt`, and tag.
