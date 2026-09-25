# Architecture

This document explains how Cull is put together: the module layout, the responsibilities of each layer, the technology choices behind it, and the reasoning for a few decisions that aren't obvious from reading the code alone.

## High-Level Shape

Cull is a **single-module** Android app (`:app` only — no `:core`/`:feature` Gradle modules) that internally follows an **MVVM + light Clean Architecture** split by package:

```
core/       — data & domain: database, datastore, model, network, grouping
feature/    — one package per screen/flow, each with Screen + ViewModel
navigation/ — type-safe nav graph, route definitions, session state
ui/         — shared design-system pieces: theme, components, icons
```

There is **no dependency-injection framework** (no Hilt/Koin). Every `ViewModel` that needs collaborators is constructed via a hand-written `ViewModelFactory` in its own file, and long-lived singletons (the SQLite helpers, the Coil `ImageLoader`, the Immich API key) are plain Kotlin `object`/companion singletons. This is a deliberate choice for a single-module app of this size: DI framework overhead (codegen, module graphs) buys little when there's only one place to wire things, and manual factories keep construction visible and debuggable.

## Layers

### `core/` — data & domain

| Package | Responsibility |
|---|---|
| `core/model/` | `PhotoItem`, `UnifiedPhotoItem` — the unified photo representation used everywhere above this layer. `UnifiedPhotoItem` carries `source` (Local/Immich), `immichAssetId`, `isOnDevice`/`isOnImmich`, and thumbnail/preview/original URLs so the UI never needs to know which backend a photo came from. |
| `core/network/` | `ImmichApi.kt` — thin OkHttp3 client for the Immich REST API (auth via `x-api-key` header, offset-based pagination, manual JSON parsing via `kotlinx.serialization`'s `JsonElement`/`JsonObject` rather than a model-mapping library). `ImmichRepository.kt` wraps it with app-facing methods (list, load original, delete). |
| `core/datastore/` | `LocalPhotoRepository.kt` — device photos via Storage Access Framework (`DocumentFile`), used when persisted folder-URI permission is granted. `UnifiedPhotoRepository.kt` — merges Local + Immich results into a single `UnifiedPhotoItem` list according to the active `SourceMode`. `SettingsRepository.kt` — Jetpack DataStore-backed preferences (see [State Persistence](#state-persistence) below); exposes every setting as a `Flow<T>`. `SettingsExport.kt` — settings export/import support. `PhotoCache.kt` — in-memory cache of the last-loaded photo list, primarily to make the Stacks feature fast without a full re-fetch. |
| `core/database/` | `ImmichAssetMappingDb.kt` — a `SQLiteOpenHelper` singleton (double-checked locking) mapping local file URIs to Immich asset IDs, so the app can tell whether a local photo also exists on the Immich server (needed for the cloud-status chip and the "device vs. device+Immich" delete breakdown). `WizardSegmentDb.kt` — a second `SQLiteOpenHelper` singleton persisting `WizardSegment` state (month key, `NOT_STARTED`/`IN_PROGRESS`/`COMPLETE`, photo/deleted counts, timestamps) for the Wizard feature. |
| `core/grouping/` | `PhotoGrouping.kt` — the time-window "stacking" algorithm (`groupPhotos()`), which groups photos taken within a configurable window (default 2 minutes) into stacks, plus `stackKey()` (an MD5 hash of the member photo IDs) used as a stable stack identity across reloads. Shared by both the Stacks feature and the Wizard's per-month stack view. |

Why raw SQLite instead of Room: two narrow, stable schemas (an ID-mapping table and a segment-state table) with simple CRUD access patterns — Room's compile-time query verification and entity mapping aren't earning their overhead here, and it keeps the project dependency-light.

Why manual JSON parsing instead of Moshi/Gson for Immich responses: the API surface consumed is small and some responses are irregularly nested (e.g., search results at `assets.items`); hand-parsing via `JsonElement` avoids maintaining DTO classes for a handful of call sites.

### `feature/` — screens & view models

Each feature package pairs a Composable `*Screen.kt` with a `*ViewModel.kt` (plus a `ViewModelFactory` for construction and, where needed, small helper files):

| Feature | Screens | ViewModel(s) | Notes |
|---|---|---|---|
| `home/` | `HomeScreen.kt` | — | Entry screen: Library / Wizard / Albums (no-op) buttons, source-mode switcher, Continue pill. |
| `library/` | `LibraryScreen.kt` | `LibraryViewModel.kt` | Grid of thumbnails, pagination (7-day rolling window for Immich), mark/pin state, date filters. Factory takes `(context, settingsRepository)`. |
| `viewer/` | `ViewerScreen.kt` (+ supporting files) | `ViewerViewModel.kt` | The core full-screen photo screen: current index, HUD visibility, pin/mark state, zoom/pan sync to the pinned photo, delete-confirmation dialog. |
| `stacks/` | `StacksScreen.kt`, `StackGridScreen.kt` | `StacksViewModel.kt` | Groups photos by the configurable time window; backed by `PhotoStackCache` (stack index → photos). |
| `wizard/` | `WizardScreen.kt` (coverage map / home), `WizardMonthScreen.kt` (per-month detail) | `WizardViewModel.kt`, `WizardMonthViewModel.kt` | See [Wizard](#wizard-feature) below. |
| `settings/` | `SettingsScreen.kt` | — (reads/writes `SettingsRepository` directly) | Source mode, folder picker, Immich URL/API key + test-connection, long-press threshold, dry-run, mirror-deletes, theme, accent hue. |

### `navigation/`

Type-safe routes are declared as `@Serializable` data objects/classes in `Route.kt` (`Home`, `Library`, `Viewer(photoIndex, folderUri)`, `Settings`, `Stacks`, `StackGrid(stackIndex)`, and the Wizard routes) and wired together in `CullNavHost.kt`. `SessionViewModel` is scoped to the nav host (survives navigation but not process death beyond what `SettingsRepository` persists) — it holds the last-viewed photo index/folder URI for the "Continue where you left off" flow and exposes a `reloadTrigger: StateFlow` that downstream screens observe to know when to refresh (e.g., after a delete changes the underlying photo set).

### `ui/`

- `ui/theme/` — `Color.kt` (Catppuccin-inspired dark/light token sets — see [Design System](#design-system-conformance) for how these map to the design spec), `Type.kt`, `Theme.kt` (builds `MaterialTheme` from the current theme + accent-hue setting).
- `ui/component/` — shared building blocks used across features: `Components.kt` (`GiantButton`, `CircleIcon`, `ContinuePill`, `SourceSwitcher` — which also defines the shared `SourceMode` enum — plus smaller layout helpers), `StackCard.kt` (stack thumbnail card, shared by Stacks and Wizard), `MarkDonePill.kt` (Wizard's "Mark done"/"Done" pill and label).
- `ui/icon/` — `CullIcons.kt`, a single object wrapping the icon set used throughout the app.

### `wizard/` feature

The Wizard (`design/wizard-prd-v1.md`) is a guided, month-by-month culling workflow layered on top of the same photo/repository/grouping infrastructure as Library and Stacks — it does not introduce a separate data source.

- **Segmentation** (`WizardSegmentation.kt`) — pure functions: `monthKey`, `countByMonth`, `monthBounds`, `monthTitle`, `monthAbbreviation`, `year`. Segments are calendar months, computed from the full photo set loaded via `WizardPhotoLoader.kt` (which reuses `UnifiedPhotoRepository` for whichever `SourceMode` is active).
- **State** (`WizardSegmentDb.kt`) — each segment persists `state` (`NOT_STARTED`/`IN_PROGRESS`/`COMPLETE`), `photoCount`, `deletedCount`, `startedAt`/`completedAt`/`lastAccessedAt`, keyed only by month — completion is global and independent of the active source mode, matching PRD §5/OQ-6. The PRD's conceptual `label` and `markedCount` fields are derived at render time rather than persisted; `bookmarkIndex` is intentionally absent (re-entering a segment always lands at the top of its stack list, per PRD §7.4).
- **Screens** — `WizardScreen.kt`/`WizardViewModel.kt` render the coverage-map home (In Progress section sorted by `lastAccessedAt`, year-grouped 3-column grid, legend, empty-state prompt); `WizardMonthScreen.kt`/`WizardMonthViewModel.kt` render a month's stat row and stack list, with the "Mark as complete" confirmation flow.
- **Caching** — `WizardScanCache.kt` avoids a full library rescan on every Wizard-home visit; `SessionViewModel.recordWizardDeletes` feeds deletion counts back into segment stats from the Viewer's delete flow.

## State Persistence

All persisted preferences live in `SettingsRepository.kt` (Jetpack DataStore), each exposed as a `Flow<T>`:

| Key | Type | Default |
|---|---|---|
| `source_mode` | SourceMode | Hybrid |
| `library_folder_uri` | String | — |
| `immich_url` | String | — |
| `immich_api_key` | String | — |
| `long_press_threshold` | Int (ms) | 220 |
| `dry_run` | Boolean | false |
| `mirror_deletes` | Boolean | false |
| `dark_theme` | Boolean | true |
| `accent_hue` | Int (0–7) | 0 |
| `last_photo_index` | Int | — |
| `grouping_window_minutes` | Int | 2 |

Wizard segment state and the local↔Immich asset mapping live in SQLite (`core/database/`) rather than DataStore, since both are keyed, queryable collections rather than flat scalar settings.

## Key Technical Flows

### Deletion

- **Local**: `MediaStore.createTrashRequest()` (API 30+) — photos are moved to the system Trash, never hard-deleted.
- **Immich**: `DELETE /api/assets` with `{ids: [...], force: false}`.
- **Dry-run mode**: both paths are skipped entirely; a toast confirms nothing was deleted.
- The delete-confirmation bottom sheet shows a breakdown (device-only vs. device+Immich) computed from `ImmichAssetMappingDb`.

### Long-press compare & zoom sync

- Implemented with `pointerInput` + `detectTapGestures(onLongPress = ...)` in the Viewer; the system long-press context menu is suppressed so the gesture is unambiguous.
- The long-press threshold is configurable (`SettingsRepository.long_press_threshold`) and passed into the gesture detector.
- Zoom/pan state (scale, offsetX, offsetY) for the current photo is tracked in `ViewerViewModel` and mirrored into a parallel "pinned transform" state on every change, so long-pressing renders the pinned photo with the same transform the user applied to the current photo.
- When zoomed in (scale > 1), horizontal swipe-navigation is disabled to avoid gesture conflicts; vertical swipe-to-mark uses nested scroll consumption to avoid colliding with the system back-swipe gesture.

### Immich integration

- Auth: an OkHttp interceptor registered in `CullApplication` injects the `x-api-key` header for image requests. `ImmichKeyGate` gates the injection — the request's scheme, host, and port must match the configured Immich base URL and the path must be an `/api/assets/` endpoint, so the key can never be sent to a foreign host. Credentials (key + base URL) are held in a companion object and updated whenever Settings changes them.
- Pagination is offset-based (`page` + `size`); there is no cursor API.
- Search uses `POST /api/search/metadata`, whose results are nested at `assets.items`.

### Image loading

`CullApplication` configures a singleton Coil 3 `ImageLoader` with an OkHttp-based fetcher (memory cache capped at 25% of available memory, disk cache capped at 2%), used for both local (`DocumentFile`/`content://`) and Immich (`https://`) image sources through the same `UnifiedPhotoItem` URLs.

## Technology Choices

| Technology | Used for | Why |
|---|---|---|
| Kotlin + Jetpack Compose | UI | Standard modern Android UI toolkit; declarative state-driven rendering fits the highly gestural, animation-heavy Viewer screen well. |
| Coil 3 | Image loading | First-class Compose support, pluggable OkHttp fetcher lets local (`content://`) and remote (Immich `https://`) images share one cache/loader. |
| OkHttp3 | Immich REST client | Lightweight, well-understood HTTP client; paired with an interceptor for auth header injection. |
| `kotlinx.serialization` (`JsonElement`) | Immich response parsing | Small, irregular API surface — avoids maintaining a full DTO layer for a handful of endpoints. |
| Jetpack DataStore | Scalar settings | Modern replacement for `SharedPreferences`; async, `Flow`-based, matches the reactive Settings screen. |
| Raw SQLite (`SQLiteOpenHelper`) | Asset-ID mapping, Wizard segment state | Two small, stable, query-simple schemas; avoids Room's codegen/entity overhead for this scale. |
| Sentry | Crash reporting | Wired via `io.sentry.android.gradle`; releases tagged with the git-commit-count version so crashes map back to a specific build. Privacy-constrained: screenshots, view-hierarchy attachment, and interaction breadcrumbs are disabled (a crash screenshot in a photo app is almost certainly a personal photo); traces sampled at 10%. |
| No DI framework | Object construction | Single-module app; manual `ViewModelFactory` per feature keeps wiring explicit without Hilt/Koin codegen overhead. |

Key versions (see `gradle/libs.versions.toml` for the full list): AGP 9.0.1, Kotlin 2.0.21, compileSdk/targetSdk 36, minSdk 34, Java 11 toolchain.

## Build, Versioning & CI

- Single Gradle module (`:app`); root `build.gradle.kts` only declares plugin versions (`apply false`).
- `versionName`/`versionCode` are **not** hand-maintained — both are derived at build time from `git rev-list --count HEAD` (a `gitCommitCount()` function in `app/build.gradle.kts`), so every commit produces a unique, monotonically increasing version. The same value is used as the Sentry release identifier and the CI release tag.
- `.github/workflows/build.yaml` runs on every push to `master`: full-history checkout (needed for the commit-count version), JDK 17 setup, `./gradlew assembleRelease` (R8-minified, signed from a base64-encoded keystore secret, cleaned up unconditionally afterward), then a GitHub Release tagged `v<version>` with the release APK and auto-generated notes. Actions are pinned to full commit SHAs, not mutable tags.
- **Known gap**: CI does not currently run `./gradlew test` or `./gradlew lint` as a separate job — only `assembleDebug`/`assembleRelease`. Tests exist (`app/src/test`, `app/src/androidTest`) but must be run locally or added to CI explicitly.

## Security Posture

Hardening applied after the 2026-07 audit (see [`SECURITY_AUDIT.md`](SECURITY_AUDIT.md)):

- **Transport**: cleartext HTTP is currently allowed app-wide (`network_security_config.xml`) so self-hosted Immich servers on plain `http://` work. The settings screen warns when an `http://` URL is entered, since the API key is then sent unencrypted. `ImmichKeyGate` still limits the key to the configured host. Set `cleartextTrafficPermitted` back to `false` to require TLS.
- **Key scoping**: `ImmichKeyGate` restricts `x-api-key` injection to asset requests whose scheme/host/port match the configured server.
- **Backups**: `files/datastore/` (which holds the API key in the Preferences DataStore) is excluded from cloud backup and device-to-device transfer in both `backup_rules.xml` and `data_extraction_rules.xml`.
- **Crash reporting**: Sentry ships no screenshots, view hierarchies, or interaction breadcrumbs; traces sampled at 10%.
- **Release builds**: R8-minified with `Log.v/d/i` stripped; only the release APK is published.
- **Input handling**: settings imports are sanitized (`SettingsExport.sanitized()`); Immich request URLs/bodies are built with `HttpUrl.Builder` and `kotlinx.serialization` rather than string concatenation.

## Testing Strategy

- **Unit tests** (`app/src/test/`) — `mockk` + `kotlinx.coroutines.test`, covering `core/network` (API parsing, repository photo-fetching/URL-generation/deletion) and feature ViewModels (`StacksViewModelTest`, `ViewerViewModelTest`).
- **Instrumented tests** (`app/src/androidTest/`) — `MockWebServer` for HTTP plus real DataStore/SQLite, covering `ImmichAssetMappingDbTest`, `SettingsRepositoryTest`, `ImmichApiMockWebServerTest`.
- No dedicated Wizard test suite was identified as of this writing — worth adding given the amount of persisted state (`WizardSegmentDb`) and state-transition logic (`NOT_STARTED → IN_PROGRESS → COMPLETE`) it owns.

## Design System Conformance

See [`docs/DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md) for the full token/spec reference and a list of known gaps between the design bundle and the current implementation (most notably: custom fonts and custom icon `ImageVector`s from the design bundle are not yet integrated — the app currently uses system `Serif`/`SansSerif` and Material default icons).
