# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Cull** (`com.rrajath.cull`) is an Android photo-culling app built with Kotlin and Jetpack Compose. Users review photos from local device storage and/or a self-hosted Immich server, pin a reference photo, long-press to compare, and swipe down to mark photos for deletion.

- **Min SDK:** Android 14 (API 34), **Target SDK:** Android 15 (API 36)
- **Build system:** Gradle with Kotlin DSL; single `:app` module
- **Build variants:** `debug` uses applicationId `com.rrajath.cull.debug`, app name "Cull Debug" (via `resValue`, so read the name from `R.string.app_name`, never hardcode it), and versionName `<version> (debug)`; it installs alongside `release`
- **No DI framework** — manual `ViewModelFactory` pattern throughout

## Commands

```bash
# Build
./gradlew build

# Unit tests (JVM)
./gradlew test

# Run a single unit test
./gradlew test --tests "com.rrajath.cull.feature.stacks.StacksViewModelTest"

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run a single instrumented test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rrajath.cull.core.database.ImmichAssetMappingDbTest

# Lint
./gradlew lint

# Debug APK
./gradlew assembleDebug
```

## Architecture

**MVVM + Clean Architecture** with three layers:

```
core/
  database/        # SQLite (no Room) for Immich↔local asset mapping
  datastore/       # DataStore preferences + PhotoCache (in-memory)
  model/           # UnifiedPhotoItem, PhotoSource, enums
  network/         # ImmichApi (OkHttp3), ImmichRepository, LocalPhotoRepository
feature/
  home/            # Entry screen, source switcher
  library/         # LazyVerticalGrid, LibraryViewModel
  viewer/          # Full-screen viewer, ViewerViewModel
  stacks/          # Time-window photo groups, StacksViewModel
  settings/        # Source mode, folder picker, Immich config
navigation/        # CullNavHost, type-safe Route definitions, SessionViewModel
ui/
  component/       # GiantButton, CircleIcon, SourceSwitcher, ContinuePill, etc.
  icon/            # CullIcons custom icon set
  theme/           # Color.kt (Catppuccin palette), Type.kt, Theme.kt
```

### Key Models

- **`UnifiedPhotoItem`** — unified local + Immich photo representation; fields include `source`, `immichAssetId`, `isOnDevice`, `isOnImmich`, thumbnail/preview/original URLs
- **`SourceMode`** — `Local` | `Immich` | `Hybrid`

### Repositories

| Repository | Responsibility |
|---|---|
| `LocalPhotoRepository` | Device photos via SAF (`DocumentFile`) |
| `ImmichRepository` | Immich API; wraps `ImmichApi` |
| `UnifiedPhotoRepository` | Merges both sources |
| `SettingsRepository` | DataStore for all 14 persisted prefs |
| `PhotoCache` | In-memory list cache for Stacks feature |

### ViewModels

- **`LibraryViewModel`** — photo loading, pagination (7-day rolling window for Immich), mark/pin state, date filters; factory takes `(context, settingsRepository)`
- **`StacksViewModel`** — groups photos by configurable time window (default 2 min); uses `PhotoStackCache` (stack index → photos)
- **`ViewerViewModel`** — full-screen viewer state: current index, HUD visibility, pin/mark, delete dialog
- **`SessionViewModel`** — nav-scoped; persists last photo index & folder URI; `reloadTrigger: StateFlow` broadcasts reloads

### Navigation

Type-safe routes via `@Serializable` data objects/classes in `navigation/Route.kt`:
- `Home`, `Library`, `Viewer(photoIndex, folderUri)`, `Settings`, `Stacks`, `StackGrid(stackIndex)`
- All wired in `CullNavHost`; `SessionViewModel` scoped to the nav host

## State Persistence (DataStore)

All keys in `SettingsRepository.kt`:

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

All exposed as `Flow<T>`.

## Feature Details

### Deletion
- **Local**: `MediaStore.createTrashRequest()` (API 30+) — never hard-deletes
- **Immich**: `DELETE /api/assets` with `{ids: [...], force: false}`
- **Dry-run mode**: skips actual deletion; shows toast
- Confirmation dialog shows counts: N on device / N on Immich / N on both

### Long-Press Compare
- Hold threshold configurable (default 220 ms); shows pinned photo full-screen
- Suppresses system context menu
- Zoom/pan of current photo mirrors to pinned photo

### Immich API (`ImmichApi.kt`)
- Auth: `x-api-key` header injected by OkHttp interceptor in `CullApplication`; `ImmichKeyGate` only allows it when the request's scheme/host/port match the configured Immich URL and the path is an `/api/assets/` endpoint
- Cleartext HTTP is currently allowed app-wide (`network_security_config.xml`, `cleartextTrafficPermitted="true"`); Settings warns that `http://` URLs send the API key unencrypted
- Pagination: offset-based (`page` + `size`), no cursor
- Search: `POST /api/search/metadata` → results at `assets.items` (nested)
- JSON parsed manually via `JsonElement`/`JsonObject` (no Moshi/Gson)

### Application Setup (`CullApplication`)
- Initializes Coil 3 singleton with OkHttp fetcher (memory cache 25%, disk 2%)
- Stores Immich credentials (API key + base URL) globally (companion object); updated when settings change

## Testing

### Unit tests (`/src/test/`)
Uses `mockk` + `kotlinx.coroutines.test`. Key test files:
- `ImmichApiTest` — API response parsing
- `ImmichRepositoryTest` — photo fetching, URL gen, deletion
- `ImmichApiRequestTest` — MockWebServer-backed request tests (URL encoding, auth header, delete body)
- `ImmichKeyGateTest` — API-key host-scoping rules
- `SettingsExportTest` — settings-import sanitization
- `StacksViewModelTest` — grouping algorithm
- `ViewerViewModelTest` — state transitions

### Instrumented tests (`/src/androidTest/`)
Uses `MockWebServer` for HTTP, real DataStore/SQLite:
- `ImmichAssetMappingDbTest` — SQLite CRUD
- `SettingsRepositoryTest` — DataStore read/write
- `ImmichApiMockWebServerTest` — full HTTP stack

## Key Files

| File | Purpose |
|---|---|
| `CullApplication.kt` | Coil setup, Immich API key storage, OkHttp interceptor |
| `navigation/CullNavHost.kt` | Navigation graph |
| `navigation/Route.kt` | All route definitions |
| `core/datastore/SettingsRepository.kt` | All persistent state |
| `core/network/ImmichApi.kt` | Immich HTTP client |
| `core/database/ImmichAssetMappingDb.kt` | Local↔Immich filename mapping (singleton, double-checked locking) |
| `ui/theme/Color.kt` | Catppuccin-based palette + `ExtendedColorScheme` |
| `core/network/ImmichKeyGate.kt` | Host-scoping gate for API-key injection |
| `gradle/libs.versions.toml` | All dependency versions |
| `docs/SECURITY_AUDIT.md` | Security audit findings and remediation status |

## Standing engineering rules (always active)

Added by the android-dev-workflow skill (`init`), adapted to this repo's conventions.

### Version control hygiene
- Fetch from the remote (`jj git fetch`) and rebase local changes onto it before making any code change. This repo uses jj; fall back to git only if a jj command fails.
- When a request implies multiple distinct changes, commit each one separately so history stays atomic and revertable. Never bundle unrelated changes into a single commit.

### Changelog discipline
- Every code change (feature, fix, refactor, dependency bump) gets an entry in `CHANGELOG.md` under `## [Unreleased]`, in the same commit as the change itself, not batched at the end of a session.
- Use Keep a Changelog sub-groups under Unreleased: `### Added`, `### Changed`, `### Fixed`, `### Removed`. Create a sub-heading if it doesn't exist yet; omit empty ones.
- Never manually move entries from `Unreleased` into a versioned section. The release workflow (`.github/workflows/release.yml`) does that on tag push; editing that boundary by hand will conflict with CI.

### Versioning
- `versionName` in `app/build.gradle.kts` is hand-maintained semver (`MAJOR.MINOR.PATCH`). `versionCode` is derived from it as `MAJOR * 10000 + MINOR * 100 + PATCH` (1.2.3 -> 10203); never set it by hand. MINOR and PATCH must stay at or below 99.

### Documentation currency
- Any code change (feature, bug fix, refactor, dependency change, structural change) should be reflected in `README.md`, `docs/ARCHITECTURE.md`, and the relevant file(s) under `docs/` in the same commit. When unsure whether a change is material enough, update rather than skip.
- Any change that touches UI (a Composable, a layout XML, a theme/style resource, a color or typography token) must follow and update the design system doc in the same commit. The existing doc is `docs/DESIGN_SYSTEM.md`; don't invent colors, spacing, or components it doesn't cover, ask instead.
- These are incremental keep-alive edits, not full regenerations. Run the `create-documentation` command periodically for a full pass that catches drift.

### Progress tracking
- Whenever a task is assigned (feature, bug fix, refactor), log it in `PROGRESS.md` at the repo root with a status (`Not started` / `In progress` / `Blocked` / `Done`) and keep the status current as work moves along.
- `PROGRESS.md` is gitignored and never committed; it is scratch space, not a deliverable.
