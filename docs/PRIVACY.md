# Airlock Privacy Policy

**Short version: Airlock collects nothing, stores nothing, and sends nothing.**

## What Airlock accesses

- **Images you explicitly pick or share** into the app, only for as long as it takes to
  produce a cleaned copy. They are processed entirely in memory / local cache on your device.
- **Text you paste or share** into the app, processed in memory on your device.

## What Airlock does NOT do

- It has **no `INTERNET` permission** and makes **no network requests**. It cannot upload,
  sync, or transmit anything. This is verifiable in the app's manifest and in your device's
  app-permissions screen.
- It uses **no analytics, crash reporting, advertising, or telemetry** SDKs.
- It keeps **no database** and **no history** of what you scrub. Cleaned images are written to
  a private cache directory only to hand them to the share sheet, and that cache is wiped every
  time the app launches.
- It requests **no storage permission** — images are chosen through the Android Photo Picker.

## Settings

Your preferences (which PII types to redact, custom tracking parameters) are stored in a local
key-value file on your device only. They never leave the device.

## Data you share onward

Once you tap **Share** to send a cleaned image or text to another app, that other app's privacy
policy applies. Airlock's job ends at producing the sanitized copy.

## Contact

Airlock is open source under Apache-2.0. Report issues on the project's issue tracker.
