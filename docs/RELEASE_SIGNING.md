# Release signing & key recovery

Airlock's release APK is signed with a single keystore. **Android requires the same signing
key for every future update** — if it is lost, you can never ship an upgrade to installed users.
This document explains how releases are signed and how the key is backed up. It contains **no
secrets** (the cert fingerprint below is public).

## Signing identity

- Key alias: `airlock`
- Algorithm: RSA 4096
- Signing certificate SHA-256: `37e2b50c7679e61ff4108f3076a128de05a7e26c548215e02e0ff500c6e5ffef`
  (also printed in each GitHub Release; use it to confirm an APK was signed by the real key:
  `apksigner verify --print-certs app-release.apk`)

## How signing is configured

`app/build.gradle.kts` reads a **gitignored** `keystore.properties` at the repo root:

```
storeFile=/absolute/path/to/airlock-release.jks
storePassword=…
keyAlias=airlock
keyPassword=…
```

If that file is absent the project still builds (unsigned release / normal debug), so the repo
stays buildable by anyone. The keystore and `keystore.properties` are never committed
(`.gitignore` blocks `*.jks`, `*.keystore`, `keystore.properties`, `*.gpg`, `*.enc`).

## Backups (defense in depth)

The signing key exists in several places so a single failure cannot lose it:

1. **Raw keystore** — `~/.android-keystores/airlock-release.jks` and a second copy under
   `~/Documents/airlock-signing/`.
2. **Encrypted portable bundle** — `airlock-keystore-backup.tar.gz.gpg` (and a `.enc` openssl
   twin) in `~/Documents/airlock-signing/` and `~/Desktop/`. Each contains the keystore,
   `keystore.properties`, and a `RECOVERY.md`. Encrypted with AES-256; the passphrase is the
   keystore `storePassword`. **These are safe to copy anywhere** (cloud, USB, password manager)
   because they are encrypted.
3. **Off-machine** — at least one encrypted bundle must live off this computer (cloud + password
   manager). That is the only thing that survives a disk failure; it is a manual step.

### Decrypt a bundle

```bash
gpg -d airlock-keystore-backup.tar.gz.gpg | tar xz
# or:
openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -in airlock-keystore-backup.tar.gz.enc | tar xz
```

### Restore on a new machine

1. Decrypt the bundle.
2. Put `airlock-release.jks` somewhere safe (e.g. `~/.android-keystores/`).
3. Put `keystore.properties` at the repo root; fix `storeFile=` to point at the jks.
4. `./gradlew :app:assembleRelease`.

## Cutting a release

```bash
./gradlew :app:assembleRelease
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk   # confirm cert
gh release create vX.Y.Z app/build/outputs/apk/release/app-release.apk --title "Airlock vX.Y.Z"
```
