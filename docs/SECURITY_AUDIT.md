# Security Audit — Cull

**Date:** 2026-07-10
**Scope:** Full repository — Android manifest, network security config, backup rules, Immich API client, secret storage and handling, settings export/import, SQLite layer, logging, CI/release pipeline, and full VCS history scan for committed secrets.

## Remediation Status (2026-07-18)

All 10 findings were addressed, one commit per finding:

| # | Finding | Resolution |
|---|---|---|
| 1 | Cleartext HTTP | **Reverted 2026-09-24 (owner decision):** cleartext allowed app-wide again so plain-HTTP Immich servers work; settings screen warns on `http://` URLs. Open risk. |
| 2 | API key in backups | `files/datastore/` excluded from cloud backup and device transfer |
| 3 | Key attached by URL substring | `ImmichKeyGate` matches scheme/host/port against the configured server |
| 4 | Sentry data collection | Screenshots, view hierarchy, and interaction breadcrumbs off; traces at 0.1 |
| 5 | Deletion logging | `Log.d` statements removed; release builds also strip `Log.v/d/i` via R8 |
| 6 | Unencoded filename in URL | Built with `HttpUrl.Builder.addQueryParameter` |
| 7 | Unvalidated settings import | `SettingsExport.sanitized()` + tolerant `sourceMode` read |
| 8 | Hand-rolled delete JSON | Built with `buildJsonObject` |
| 9 | Release hardening | R8 enabled with log stripping. **Debug APK re-published 2026-09-24 (owner decision)** as `cull-v<version>-debug.apk` on GitHub Releases; debuggable, debug-signed build is public again. Open risk. |
| 10 | CI supply chain | Actions pinned to commit SHAs; Sentry inbound filters remain a project-side setting |

Deliberate accepts: `includeSourceContext` stays enabled (source exposure to Sentry accepted); the DSN stays client-embedded (by design).

---

## High Severity

### 1. Cleartext HTTP permitted globally

**Location:** `app/src/main/res/xml/network_security_config.xml:3`

**Finding:** The network security config sets `cleartextTrafficPermitted="true"` in the base config, allowing unencrypted HTTP to any host.

**Impact:** If the user configures an `http://` Immich URL (common for self-hosted LAN setups), the `x-api-key` header is sent unencrypted on every request. Anyone on the same network can capture a key that carries full read and delete rights over the user's photo library.

**Recommendation:**
- Remove the global cleartext flag.
- If LAN support is required, allow cleartext only for specific domains via `<domain-config>` (e.g. RFC 1918 hostnames the user configures), or
- At minimum, make the settings screen warn loudly when the entered server URL uses `http://`.

---

### 2. Immich API key can leave the device via Android backups

**Location:** `core/datastore/SettingsRepository.kt:26`, `app/src/main/AndroidManifest.xml:10`, `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`

**Finding:** The API key is stored in plaintext DataStore. The manifest sets `android:allowBackup="true"`, and both backup rules files are the unedited templates (all rules commented out), so the entire app data directory — including the DataStore file containing the key — is included in Google cloud backup and device-to-device transfer.

**Impact:** The key leaves the device despite the deliberate design decision in `SettingsExport` to exclude it ("secret, must never leave the device"). It becomes readable from cloud backup contents or a transferred device.

**Recommendation:**
- Add `<exclude domain="file" path="datastore/"/>` to both `backup_rules.xml` and `data_extraction_rules.xml`, or
- Store the key encrypted via Android Keystore / `EncryptedSharedPreferences` so backed-up bytes are useless off-device.

---

## Medium Severity

### 3. API key attached by URL substring, not host

**Location:** `CullApplication.kt:37`

**Finding:** The Coil OkHttp interceptor adds the `x-api-key` header to any request whose URL merely *contains* `/api/assets/`, with no verification that the request host is the configured Immich server.

**Impact:** Latent key-exfiltration gap. Today all image URLs are app-constructed, so there is no active leak — but any future feature that loads an externally-influenced URL (shared link, remote config, server-provided redirect) would send the key to an arbitrary host.

**Recommendation:** Compare `request.url.host` against the host of the configured Immich URL before attaching the key.

---

### 4. Sentry configured to ship user photos and source code off-device

**Location:** `app/src/main/AndroidManifest.xml:41` (and surrounding meta-data), `app/build.gradle.kts:123`

**Finding:**
- `io.sentry.attach-screenshot` is `true` — a crash captures a screenshot of whatever is on screen.
- `io.sentry.attach-view-hierarchy` is `true`.
- `io.sentry.traces.sample-rate` is `1.0` with user-interaction breadcrumbs enabled.
- `includeSourceContext.set(true)` uploads application source code to Sentry.

**Impact:** In a photo-culling app, a crash screenshot is almost certainly a user's personal photo, uploaded to a third-party service. Full tracing plus interaction breadcrumbs records detailed usage. This conflicts with the privacy posture implied by supporting self-hosted Immich.

**Recommendation:** Disable `attach-screenshot` (at minimum), reconsider view-hierarchy attachment, and lower the trace sample rate for production. Keep `includeSourceContext` only if source exposure to Sentry is acceptable.

---

### 5. Verbose deletion logging survives into release builds

**Location:** `core/network/ImmichApi.kt:258`, `core/network/ImmichApi.kt:269`; `app/build.gradle.kts:55` (`isMinifyEnabled = false`)

**Finding:** `deleteAssets` logs the full request URL, request body, asset IDs, and response body at `Log.d`. Because the release build is not minified, no log stripping occurs and these statements execute in production.

**Impact:** Asset IDs and server URLs are readable via `adb logcat` by anyone with debugging access to the device; deletion activity is recorded in the system log.

**Recommendation:** Remove the log statements or gate them on `BuildConfig.DEBUG`; enabling R8 with a `Log` stripping rule also addresses this (see finding 9).

---

### 6. Unencoded filename interpolated into URL

**Location:** `core/network/ImmichApi.kt:287` (`checkAssetExists`)

**Finding:** `fileName` is string-interpolated directly into the query string: `"$baseUrl/api/search/metadata?originalFileName=$fileName&type=IMAGE&size=1"`.

**Impact:** Filenames containing `&`, `#`, `+`, `?`, or spaces break the query or inject additional query parameters. Server-side effect is limited (read-only search endpoint), but results are silently wrong for affected filenames.

**Recommendation:** Build the URL with OkHttp's `HttpUrl.Builder.addQueryParameter()`, which percent-encodes values.

---

## Low Severity

### 7. Settings import accepts unvalidated values

**Location:** `core/datastore/SettingsRepository.kt:216` (`importSettings`), `core/datastore/SettingsRepository.kt:42` (`SourceMode.valueOf`)

**Finding:** `importSettings` persists `export.sourceMode` verbatim. The `sourceMode` flow later calls `SourceMode.valueOf(mode)`, which throws `IllegalArgumentException` on any unexpected string. Numeric fields (`accentHue`, `longPressThresholdMs`, `groupingWindowMinutes`) are also persisted without range checks.

**Impact:** A malformed or malicious settings JSON puts the app into a crash loop on next read, recoverable only by clearing app data (denial of service via crafted import file).

**Recommendation:** Validate the enum with `SourceMode.entries.find { it.name == ... }` (falling back to a default) and clamp numeric fields before persisting.

---

### 8. Hand-rolled JSON construction in `deleteAssets`

**Location:** `core/network/ImmichApi.kt:256-257`

**Finding:** The delete request body is built by string concatenation: `assetIds.joinToString(",") { "\"$it\"" }`.

**Impact:** Low — asset IDs are server-issued UUIDs, so injection is unlikely in practice, but the pattern is fragile.

**Recommendation:** Use `buildJsonObject` / `kotlinx.serialization`, as `searchMetadata` in the same file already does.

---

### 9. Release build hardening gaps

**Location:** `app/build.gradle.kts:55`, `.github/workflows/build.yaml:59-68`

**Finding:**
- Release build has `isMinifyEnabled = false` — no R8 shrinking, obfuscation, or log stripping.
- The CI workflow publishes the **debug** APK (debuggable, debug-signed) to public GitHub Releases alongside the release APK.

**Impact:** Easier reverse engineering; debug logging retained (see finding 5); a publicly downloadable debuggable build.

**Recommendation:** Enable R8 for release (with a `-assumenosideeffects` rule for `android.util.Log`), and stop attaching the debug APK to public releases.

---

### 10. CI supply chain and Sentry DSN notes

**Location:** `.github/workflows/build.yaml`, `app/src/main/AndroidManifest.xml:32`

**Finding:**
- GitHub Actions are pinned by mutable tag (`actions/checkout@v4`, `actions/setup-java@v4`, `gradle/actions/setup-gradle@v4`) rather than commit SHA.
- The Sentry DSN is hardcoded in the manifest. DSNs are designed to be client-embedded, but a public DSN allows anyone to spam events into the Sentry project.

**Impact:** Tag-pinned actions can be silently repointed if an upstream repo is compromised. DSN exposure is a nuisance (quota exhaustion / noise), not a data risk.

**Recommendation:** Pin actions to full commit SHAs. Optionally enable Sentry inbound filters / rate limits for the project.

---

## What Looks Good

- **SQLite:** all queries in `ImmichAssetMappingDb` and `WizardSegmentDb` use parameterized bindings — no injection surface.
- **Component exposure:** the only exported component is the launcher activity; no deep links, no `onNewIntent` handling, no content providers or services.
- **Deletion safety:** local deletes go through `MediaStore.createTrashRequest()` (never hard-deletes); Immich deletes use `force: false` (trash, recoverable); dry-run mode exists.
- **Settings export:** `SettingsExport` deliberately excludes the API key and device-local URIs.
- **CI secrets:** keystore is decoded to `$RUNNER_TEMP` and removed with `if: always()`; secrets are passed only via env vars.
- **History:** no hardcoded secrets found anywhere in the VCS history.

## Suggested Fix Order

1. Findings **1 + 2** together — they compound (plaintext key + cleartext transport + cloud backup).
2. Finding **4** — privacy posture (Sentry screenshots).
3. Findings **3, 5, 6** — small, contained code fixes.
4. Findings **7–10** — hardening and hygiene.
