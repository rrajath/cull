# Cull

**Cull** is a native Android photo-culling app. Pin a reference photo, long-press any other photo to compare it against the pin, swipe down to mark for deletion — all full-screen, one photo at a time.

## The Problem

There isn't a good photo-culling app for Android. Culling — deciding which photos to keep and which to delete — works best when you can hold one photo as a reference and flip through the rest to compare against it. On a desktop this is easy: a split screen shows both images side by side. On a phone, a split screen is a bad compromise — each image gets cropped or shrunk down to the point where comparison is meaningless.

Cull solves this with a **press-and-hold compare** gesture instead of a permanent split screen: pin a photo, then long-press any other photo to flash the pinned photo full-screen for as long as you hold. Let go and you're back to the photo you were on. You get true side-by-side-quality comparison without ever giving up full-screen real estate.

It also supports two sources of photos — your device's local storage and a self-hosted [Immich](https://immich.app) server — and can mirror deletions across both ("Hybrid" mode), so culling stays in sync between phone and server.

## Features

- **Home** — two entry points: **Library** (browse everything) and **Wizard** (structured, month-by-month culling).
- **Full-screen viewer** — swipe left/right to move between photos, with a loading spinner while full-res images load.
- **Pin & compare** — pin one photo as a reference; long-press any other photo to reveal the pinned photo for as long as you hold, then release to return. Long-pressing the pinned photo itself is a no-op (with a toast explaining why).
- **Pinch-to-zoom with mirrored pan/zoom** — zoom and pan the current photo, and the pinned photo mirrors the same transform, so you're always comparing the same region of both images. A "Reset both zooms" button appears whenever either image is zoomed.
- **Swipe-to-mark** — swipe down on a photo to mark it for deletion (shown greyscale, with an undo option). Swipe down again to unmark.
- **Batch delete with confirmation** — a HUD shows how many photos are marked; confirming shows a bottom sheet with a breakdown of "on device only" vs. "on device + Immich" before deleting.
- **Trash, never hard-delete** — local deletions use `MediaStore.createTrashRequest()` (Android 11+); nothing is permanently deleted from the device. Immich deletions call its `/api/assets` delete endpoint.
- **Dry-run mode** — rehearse a culling session without deleting anything; a toast confirms nothing was touched.
- **Local, Immich, or Hybrid source modes** — cull a local folder, a remote Immich library, or both at once with delete-mirroring between them.
- **Stacks** — photos taken within a short time window (configurable) are grouped into "stacks" so bursts and near-duplicates are easy to spot and compare.
- **Wizard** — a guided, session-based way to work through an entire library month by month. A coverage-map grid shows which months are Not Started, In Progress, or Complete, so you always know where you left off and what's left to review.
- **Continue where you left off** — the app remembers your last-viewed photo and folder, and surfaces a "Continue" shortcut on Home.
- **Configurable long-press threshold, dark/light theme, and accent color** — tune the compare gesture timing and personalize the look in Settings.

## Screenshots

_Not yet captured — add screenshots to `docs/screenshots/` and link them here._

## Getting Started

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (recent stable channel)
- JDK 17
- An Android device or emulator running **API 34 (Android 14)** or newer
- (Optional) A running [Immich](https://immich.app) server + API key, if you want to test Immich/Hybrid mode

### Clone

```bash
git clone <repository-url>
cd occullt
```

### Build

```bash
./gradlew build
```

### Run

Open the project in Android Studio and run the `app` configuration on a device/emulator, or from the command line:

```bash
./gradlew installDebug
```

On first launch, choose **Library** and pick a folder via the system folder picker (local mode), or configure an Immich server URL and API key in **Settings** (Immich/Hybrid mode). Prefer an `https://` server URL. Cleartext `http://` is currently allowed, but the API key is then sent unencrypted and Settings shows a warning.

### Test

```bash
# Unit tests (JVM)
./gradlew test

# Run a single unit test
./gradlew test --tests "com.rrajath.cull.feature.stacks.StacksViewModelTest"

# Instrumented tests (requires a connected device/emulator)
./gradlew connectedAndroidTest
```

### Lint

```bash
./gradlew lint
```

### Build a release APK

```bash
./gradlew assembleRelease
```

Release builds are signed and Sentry-enabled; see [Versioning & Releases](#versioning--releases) below for how CI produces official builds.

## Versioning & Releases

Cull doesn't use a hand-maintained version number. `versionName` and `versionCode` are both derived at build time from the total commit count on the branch (`git rev-list --count HEAD`), so every commit to `master` gets a unique, monotonically increasing version automatically.

On every push to `master`, [`.github/workflows/build.yaml`](.github/workflows/build.yaml):

1. Computes the version from commit count.
2. Builds `assembleRelease` (R8-minified, signed using a keystore secret).
3. Uploads build/release info to [Sentry](https://sentry.io) for crash reporting, tagged with the computed version.
4. Publishes a GitHub Release tagged `v<version>` with the release APK attached and auto-generated release notes.

There is no separate CI job for tests/lint today — see [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for details and known gaps.

## Documentation

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — app architecture, module layout, and technology choices
- [`docs/DESIGN_SYSTEM.md`](docs/DESIGN_SYSTEM.md) — design tokens, screen specs, and component inventory
- [`CLAUDE.md`](CLAUDE.md) — developer/agent guide to the codebase (commands, architecture summary, key files)

## Tech Stack

Kotlin, Jetpack Compose, Coil 3 (image loading), OkHttp3 (Immich REST client), DataStore (preferences), raw SQLite (local↔Immich asset mapping, Wizard segment state), Sentry (crash reporting). No dependency-injection framework — manual `ViewModelFactory` wiring throughout. See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full breakdown and rationale.

## License

No license file is currently included in this repository. All rights reserved by the author unless a license is added.
