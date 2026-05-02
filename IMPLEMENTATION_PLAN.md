# Implementation Plan: Cull - Android Photo Culling App

## Tech Stack & Architecture
- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Min SDK:** API 34 (Android 14)
- **Architecture:** MVVM + Clean Architecture (Presentation, Domain, Data layers)
- **Image Loading:** Coil 3
- **Persistence:** Jetpack DataStore (Preferences + Proto)
- **Async:** Kotlin Coroutines + Flow
- **DI:** Hilt

---

## Milestone 1: Foundation & Home Screen (~2-3 days)

**Goal:** Set up the project structure, design system, and home screen with navigation.

### Tasks:
1. **Project Setup**
   - Create Android project (minSdk 34, targetSdk 34)
   - Configure Compose, Hilt, DataStore, Coil dependencies
   - Set up Clean Architecture module structure (`:app`, `:core:domain`, `:core:data`, `:feature:home`, `:feature:library`, `:feature:viewer`, `:feature:settings`)

2. **Design Tokens**
   - Implement `Theme.kt` with dark/light themes
   - Define Color.kt with all design tokens (bg, bgElev, accent, danger, pin, scrim, etc.)
   - Define Type.kt with typography (Instrument Serif italic for headings, Geist for UI)
   - Define Dimens.kt with spacing scale (4dp base: 4, 8, 12, 16, 20, 24, 28, 32, 40, 48)
   - Define Shapes.kt with radii (32dp giant buttons, 22dp settings cards, 14dp thumbnails, 999dp pills)

3. **Shared UI Components**
   - Implement icon set (17 SVG icons from design: IPin, ITrash, ICloud, ICloudOff, ICheck, IX, IUndo, ISettings, IFolder, IImage, IAlbums, IZoomReset, etc.)
   - Implement reusable components: GiantButton, CircleIcon, ContinuePill, SourceSwitcher, SectionLabel, Div, Row, ToggleRow, SliderRow

4. **DataStore Setup**
   - Define preferences schema: source mode, library folder URI, Immich URL/API key, long-press threshold, dry-run toggle, dark theme, accent hue, last viewed photo index, marked IDs, pinned ID
   - Implement `SettingsRepository` with DataStore

5. **Navigation**
   - Set up Compose Navigation with routes: `home`, `library`, `viewer`, `settings`
   - Implement nav host with transitions

6. **Home Screen**
   - Implement Home screen UI: CullMark header, hero text ("Keep only the best."), source switcher pill (Local / Local+Immich / Immich), Library giant button, Albums button (disabled), Continue pill (conditional), version footer
   - Wire up Settings navigation
   - Wire up Library navigation
   - Implement Continue pill logic (read last session state from DataStore)

---

## Milestone 2: Library Grid & Viewer Core (~4-5 days)

**Goal:** Implement photo loading from local storage, thumbnail grid, and full-screen viewer with navigation.

### Tasks:
1. **Local Photo Access (Data Layer)**
   - Implement SAF folder picker flow (`ACTION_OPEN_DOCUMENT_TREE`)
   - Implement `takePersistableUriPermission` for folder access
   - Implement `LocalPhotoRepository` using DocumentFile + ContentResolver
   - Build thumbnail loading pipeline with Coil
   - Build full-resolution image loading pipeline

2. **Library Screen**
   - Implement app bar: back button, "Library" title, source badge, subtitle
   - Implement `LazyVerticalGrid` (3 columns, 6dp gap, 12dp padding)
   - Implement thumbnail cells (3:4 aspect ratio, r=14, CoilImage)
   - Add overlays: pin badge (top-left), deletion indicator (top-right + grayscale)
   - Implement floating HUD trigger pill ("Tap for HUD")
   - Wire up click to navigate to Viewer

3. **Viewer Shell**
   - Implement full-screen edge-to-edge photo viewer
   - Implement horizontal swipe navigation (prev/next) with pager
   - Implement index counter (top-center, "0142 / 1,208" format)
   - Implement gradient scrims (top/bottom)
   - Implement loading indicator (46dp spinning ring)

4. **Pin System**
   - Implement pin state management in ViewModel
   - Implement pin badge on images (top-left, 40dp circle + "PINNED" label)
   - Implement pin persistence (only one pin at a time, survives scroll)
   - Implement pin icon in HUD

5. **HUD (Pill Variant)**
   - Implement HUD state (open/closed)
   - Implement HUD Pill layout: Row 1 (cloud status chip), Row 2 (marked count + pin button + confirm button)
   - Implement tap gesture to toggle HUD
   - Implement HUD trigger pill when closed

---

## Milestone 3: Gestures, Marking, Zoom (~4-5 days)

**Goal:** Implement all core culling gestures and interactions.

### Tasks:
1. **Long-Press Compare**
   - Implement long-press gesture with configurable threshold (default 220ms)
   - Suppress system long-press context menu
   - Show pinned image full-screen while long-press held
   - Return to current image on release
   - Implement toast: "Cannot compare a pinned image against itself"
   - Handle HUD visibility during long-press (hide HUD, show pinned)

2. **Swipe-Down Mark for Deletion**
   - Implement swipe-down gesture (>=60dp dominant vertical)
   - Implement animation (translateY 18dp + grayscale + brightness 0.85)
   - Implement MarkedBanner (top-right danger pill with Undo button)
   - Toggle mark/unmark on repeated swipes
   - Persist marked IDs in ViewModel

3. **Pinch-to-Zoom & Zoom Sync**
   - Implement pinch-to-zoom gesture
   - Implement pan gesture while zoomed
   - Track scale + offsetX + offsetY in ViewModel
   - Mirror zoom/pan transforms to pinned image
   - Disable horizontal swipe when zoomed (scale > 1)

4. **Reset Both Zooms Button**
   - Implement button (top-right, translucent)
   - Show only when zoom > 1x
   - Reset both current and pinned image zoom/pan to default

5. **Settings Screen**
   - Implement source radio cards (Local / Immich / Local+Immich)
   - Implement folder picker row with path display
   - Implement Immich URL + API key rows (masked)
   - Implement connection status + Test Connection button (stub for now)
   - Implement long-press threshold slider (80-800ms, default 220)
   - Implement dry-run toggle
   - Implement mirror-deletes toggle (hidden for local-only mode)
   - Implement dark theme toggle
   - Implement accent hue picker (5 swatches)
   - Wire all settings to DataStore

---

## Milestone 4: Deletion Flow, Session Persistence, Polish (~2-3 days)

**Goal:** Complete the deletion flow, session resume, and polish the app.

### Tasks:
1. **Delete Confirmation Dialog**
   - Implement bottom sheet dialog (r=28, scrim 0.55)
   - Display count of marked photos
   - Implement summary card (photos on device only vs device + Immich)
   - Implement Cancel + "Move to Trash" buttons
   - Implement actual deletion via `MediaStore.createTrashRequest()` (API 30+)
   - Implement dry-run mode: show toast "Dry run mode enabled. No photos were deleted"

2. **Session Persistence**
   - Save last viewed photo index + folder URI on app background/exit
   - Implement "Continue where you left off" button on Home screen
   - Restore marked IDs and pinned ID on session resume

3. **Light Theme**
   - Implement light theme color tokens
   - Test all screens in light mode

4. **HUD Variants (Rail + Bar)**
   - Implement HUDRail (right-side vertical pill)
   - Implement HUDBar (full-width bottom gradient bar)
   - Add HUD style preference in Settings (optional)

5. **Polish & Testing**
   - Test all gesture conflicts (system back swipe vs mark swipe, zoom vs pan)
   - Test edge cases: empty library, single photo, all marked, pin on last photo
   - Performance: lazy loading, thumbnail caching, memory management
   - Accessibility: content descriptions, touch targets >= 48dp
   - App icon: implement adaptive icon (choose from 5 design variants)

---

## Milestone 5: Immich Integration (Future) (~3-4 days)

**Goal:** Add Immich support for hybrid and Immich-only modes.

### Tasks:
1. **Immich Client**
   - Implement REST API client with API key auth
   - Implement `GET /api/assets` for asset listing
   - Implement `GET /api/assets/{id}/original` for full-res images
   - Implement `DELETE /api/assets` for deletion

2. **Immich Integration**
   - Wire Immich client to Library and Viewer
   - Implement cloud status chip per image (on Immich / local only / missing)
   - Implement Test Connection in Settings

3. **Hybrid Mode**
   - Merge local + Immich photo lists
   - Implement cloud chip visibility logic
   - Implement mirror deletes to Immich toggle
   - Update delete confirmation to show split counts (local only vs local + Immich)

---

## Key Technical Decisions

| Decision | Rationale |
|---|---|
| API 34 min | Clean trash API, modern photo permissions, no legacy code paths |
| Clean Architecture | Testable, maintainable, easy to add Immich later without touching UI |
| DataStore over SharedPreferences | Type-safe, async, coroutine-friendly |
| Coil 3 | Best Compose integration, supports SAF URIs and remote URLs |
| Compose Navigation | Standard for Compose apps, type-safe with type-safe navigation |
| Hilt DI | Reduces boilerplate, integrates well with ViewModel |

## Risk Areas

1. **Gesture conflicts:** Horizontal swipe vs system back gesture, vertical swipe vs mark gesture. Need careful nested scroll handling.
2. **Zoom sync:** Mirroring zoom/pan transforms to pinned image needs precise state management.
3. **SAF permissions:** Persistable URI permissions can be tricky across app restarts.
4. **Memory management:** Loading full-res images in a pager needs careful bitmap recycling.

## Design Reference

- Design tokens, colors, typography, and component specs: `design/README.md`
- Interactive HTML prototype: `design/CULL - Android photo culler.html`
- Original PRD: `design/original-prd.md`
- Screen mockups: `design/screens/`
- Icon definitions: `design/components/icons.jsx`
- Theme definitions: `design/components/theme.jsx`
