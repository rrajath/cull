# Changelog

All notable changes to this project are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- Debug build variant that installs alongside release: package `com.rrajath.cull.debug`, shown as "Cull Debug", version suffixed with " (debug)".
- Settings warns when an `http://` Immich URL is entered, since the API key is then sent unencrypted.
- Tag-triggered release workflow (`v*` tags) that builds both APKs, publishes a GitHub Release with notes from this changelog, and moves Unreleased entries into the version section.
- Security audit report (`docs/SECURITY_AUDIT.md`).

### Changed

- Package and application ID renamed from `com.rrajath.occullt` to `com.rrajath.cull`. This installs as a new app; settings from the old app do not carry over.
- Versioning is now hand-maintained semver; `versionCode` is derived as `MAJOR * 10000 + MINOR * 100 + PATCH`.
- Pushes to `master` now only build and validate; releases are cut by pushing a version tag.
- CI release APKs are named `cull-v<version>-<variant>.apk`, and both the release and debug APKs are attached to GitHub Releases.
- Release builds are minified with R8 and resource shrinking, with verbose logging stripped.
- The Immich API key is only sent to the configured server's host, scheme, and port.
- App data (DataStore, including the Immich API key) is excluded from Android cloud backup and device transfer.
- Settings import validates and sanitizes imported values.
- Sentry no longer captures screenshots, view hierarchy, or full performance traces.
- GitHub Actions are pinned to commit SHAs.

### Fixed

- Filenames are percent-encoded when checking whether an asset already exists on Immich.

### Removed

- Verbose deletion logging from the Immich client.
