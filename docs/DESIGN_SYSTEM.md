# Design System — CULL

## Overview

CULL is an Android photo-culling app. Core interaction: pin a reference image, long-press any other image to compare, swipe down to mark for deletion. Supports local folder culling and Immich self-hosted integration, plus a guided **Wizard** flow for working through a library month by month.

This file is the living, in-repo design reference — read it before adding, modifying, or styling any UI element, and update it whenever a design/styling change ships. It supersedes the original hand-off bundle at `design/DESIGN_SYSTEM.md`; that file still has the original React/HTML prototypes and PRD it was authored against, but this file reflects what's actually implemented and adds the Wizard spec that the original bundle predates.

**Fidelity target:** match colors, type, spacing, radii, and gestures precisely to what's specified below.

---

## Implementation Status (read this first)

A few gaps exist between the original design hand-off and the current app. Know these before assuming a token or asset is available:

| Item | Spec | Current implementation | Action |
|---|---|---|---|
| Display/hero font | Instrument Serif (Google Fonts) | `FontFamily.Serif` (system default) | Not bundled. Add `Instrument Serif` to `res/font/` and wire it into `Type.kt` if pixel-accurate type is required. |
| UI body/label font | Geist (Google Fonts) | `FontFamily.SansSerif` (system default) | Not bundled. Add `Geist` to `res/font/` and wire it into `Type.kt`. |
| Icons | Custom 24×24 `ImageVector`s recreated from `components/icons.jsx` | `CullIcons.kt` wraps Material default icons (`Icons.Default.*` / `Icons.AutoMirrored.Filled.*`) | Visually close for most icons but not pixel-identical to the design bundle's custom glyphs. Recreate as custom `ImageVector`s if exact icon fidelity is required. |
| Core color tokens (`bg`, `bgElev`, `bgElev2`, `line`, `lineStrong`, `fg`, `fgDim`, `fgFaint`, `scrim`, `danger`, `dangerSoft`) | See table below | Implemented in `Color.kt` and match spec (dark and light) | Conformant. |
| Accent / accentSoft / pin colors (hue-driven, `oklch`) | Computed per user-selected hue | Accent hue is a persisted setting (`accent_hue`, 0–7); confirm exact color derivation in `Theme.kt`/`Color.kt` against the oklch formula below before shipping a new hue swatch | Verify when touching theming code. |
| Wizard screen design | Not present in the original design bundle (feature added after the bundle was authored) | Implemented (`feature/wizard/`) | This doc adds a Wizard section (§6) below, styled to match the existing token/spacing/radii system — treat it as the canonical spec going forward. |

---

## Design Tokens

### Colors — Dark (default)

| Token | Value | Usage |
|---|---|---|
| bg | #0d0d0f | Screen background |
| bgElev | #17171a | Cards, elevated surfaces |
| bgElev2 | #1f1f23 | Secondary cards, inputs |
| line | rgba(255,255,255,0.08) | Borders, dividers |
| lineStrong | rgba(255,255,255,0.16) | Stronger borders |
| fg | #f3f2ef | Primary text |
| fgDim | rgba(243,242,239,0.66) | Secondary text |
| fgFaint | rgba(243,242,239,0.38) | Disabled/tertiary |
| accent | oklch(0.74 0.18 {hue}) ~= #E8854A | Brand, CTAs, active |
| accentSoft | oklch(0.28 0.06 {hue}) ~= #3A2318 | Accent tint bg |
| danger | oklch(0.66 0.22 25) ~= #D94F35 | Deletion, destructive |
| dangerSoft | oklch(0.32 0.12 25) ~= #4A1F18 | Danger tint bg |
| pin | oklch(0.82 0.18 {hue}) ~= #F5A060 | Pin indicator |
| scrim | rgba(0,0,0,0.55) | Dialog scrim |
| wizardComplete | accent (filled) | Coverage-map "Complete" month fill — see §6 |
| wizardInProgress | accent (border only) | Coverage-map "In Progress" month border — see §6 |
| wizardNotStarted | bgElev2, near-invisible | Coverage-map "Not Started" month fill — see §6 |

### Colors — Light

| Token | Value |
|---|---|
| bg | #f6f4ef |
| bgElev | #ffffff |
| bgElev2 | #eeeae2 |
| fg | #14120e |
| fgDim | rgba(20,18,14,0.64) |
| fgFaint | rgba(20,18,14,0.40) |
| accent | oklch(0.62 0.19 {hue}) ~= #C06830 |
| danger | oklch(0.56 0.22 25) ~= #B83820 |

> **Accent hue** is user-configurable, persisted in DataStore as `accent_hue` (0–7). Default = 40 (tangerine). Options: 40, 25, 150, 220, 300. Rebuild `MaterialTheme` on change. Convert oklch to sRGB at https://oklch.com.

### Typography

| Role | Font | Weight | Size | Notes |
|---|---|---|---|---|
| Display/hero | Instrument Serif (spec) / system Serif (current) | 400 italic | 44–88sp | letterSpacing -1 to -2 |
| Screen title | Instrument Serif (spec) / system Serif (current) | 400 italic | 28–32sp | letterSpacing -0.6 |
| UI body | Geist (spec) / system SansSerif (current) | 400 | 13–15sp | |
| UI label | Geist (spec) / system SansSerif (current) | 600–700 | 12–14sp | letterSpacing 0.2 |
| Section header | Geist (spec) / system SansSerif (current) | 700 | 11sp | 1.6sp, uppercase |
| Numerics | Geist (spec) / system SansSerif (current) | 600–800 | varies | tabular-nums |

See [Implementation Status](#implementation-status-read-this-first) — both display fonts are currently system fallbacks, not the bundled Google Fonts.

### Radii

| Usage | dp |
|---|---|
| Screen corner | 42 |
| Giant home buttons | 32 |
| Settings cards | 22 |
| Thumbnails | 14 |
| Toasts | 14 |
| Pill/circle | 999 |
| Wizard month cell | 12 |
| Wizard "In Progress" card | 22 |

### Spacing

Base = 4dp. Scale: 4, 8, 12, 16, 20, 24, 28, 32, 40, 48.

---

## Source Mode

App-wide, stored in DataStore (`source_mode`). Chosen on Settings > Source. Defined as the shared `SourceMode` enum in `ui/component/Components.kt`.

| Mode | Description |
|---|---|
| local | Device folder only. Immich not used. Cloud chip hidden in HUD. |
| immich | Cull Immich library directly. No local files. Mirror toggle hidden. |
| hybrid *(default)* | Device folder + mirror deletions to Immich. Cloud chip shown per image. |

Source mode is global and shared across Library, Stacks, and Wizard — switching modes does not reset Wizard segment completion state (segments are keyed by month only, not by source mode).

---

## Screens

### 1. Home

**Layout** (full-screen column, padding 8/20/20dp):
- Header row: CullMark wordmark (Instrument Serif italic) left + Settings circle button (44dp, bgElev bg) right
- Hero: Instrument Serif italic 54sp, lineHeight 0.92, letterSpacing -1.2. Line 1 fg, Line 2 accent.
- Body: 14sp fgDim, max-width 280dp
- Source switcher pill: 3 segments (Local / Local+Immich / Immich). bgElev bg + line border + borderRadius 999. Active segment: accent bg + dark text. 12sp 700 weight. Each segment 9dp vertical padding.
- Giant buttons (marginTop: auto, gap 14dp):
  - Library: accent bg, r=32, minHeight 160. Instrument Serif italic 44sp + 13sp sub + 44dp arrow circle (rgba(13,13,15,0.12) bg).
  - Wizard: a giant button alongside Library, entry point to the coverage-map flow (see §6). Style consistently with the Library/Albums giant-button treatment — bgElev bg + line border, r=32, unless product direction calls for accent treatment to promote it.
  - Albums: bgElev bg + line border, r=32, minHeight 118, opacity 0.55. Currently a no-op.
- Continue pill (if session exists): 54dp thumbnail (r=16) + label + 40dp accent arrow circle. bgElev2 bg, lineStrong border, r=24.

### 2. Library

**Layout**:
- App bar: back circle (44dp) + title 'Library' (Instrument Serif italic 28sp) + source badge (10sp uppercase accent, accentSoft bg, r=6) + 12sp fgDim subtitle
- LazyVerticalGrid: 3 cols, 6dp gap, 12dp horizontal padding
- Cell: 3:4 aspect, r=14, CoilImage
  - Pinned overlay: 26dp circle (pin bg) top-left +6dp
  - Deleted overlay: 22dp circle (danger bg) top-right +6dp + grayscale filter on image
- Floating HUD trigger pill: bottom 40dp, centered

### 3. Viewer (core screen)

Full-screen edge-to-edge photo. All overlays float.

Gradient scrims: top rgba(0,0,0,0.5)→transparent 10%; bottom transparent 80%→rgba(0,0,0,0.55).

**Gestures:**

| Gesture | Action |
|---|---|
| Horizontal swipe | Navigate prev/next photo |
| Swipe down >=60dp dominant vertical | Toggle marked-for-deletion |
| Tap (no movement, <250ms) | Toggle HUD open/close |
| Long press (configurable, default 220ms) | While held: show pinned photo. Release: return to current. Suppress system long-press menu. |
| Long press on the pinned photo | Toast 'Cannot compare a pinned image against itself'. No-op. |
| Pinch to zoom | Zoom current. Mirror transform (scale + pan) to pinned image in memory. |
| Pan while zoomed | Mirror pan to pinned. |

**Index counter:** top-center absolute. Format '0142 . 1,208'. rgba(0,0,0,0.55)+blur, pill, 12sp 600.

**HUD closed pill:** bottom-center. Accent count badge + 'Tap for HUD'. rgba(16,16,18,0.76)+blur, pill.

**HUD open (pill variant — default):** Two rows, bottom 18dp:
1. Cloud status chip (NON-INTERACTIVE, pointerEvents=none): rgba(0,0,0,0.42)+blur, pill, 10.5sp uppercase. 'On Immich' (ICloud) or 'Local only' (ICloudOff, dimmer). Hidden if source=local.
2. Action strip: rgba(16,16,18,0.86)+blur, pill, 6dp padding. Left: 28dp danger circle + '{N} marked'. Right: pin button (40dp, pin bg when active) + Confirm (accent bg, pill).

**HUD variants:** Rail (right-side vertical pill, vertically centered) and Bar (full-width bottom gradient) — see design bundle's `viewer.jsx` and canvas Section 03.

**Pin badge** (this photo is pinned): top-left 18dp/16dp. 40dp pin circle + IPinFilled. 'PINNED' label 11sp 700 uppercase white.

**Marked state:** grayscale+brightness(0.85) + translateY(18dp) anim. Top-right danger pill: 'Marked for deletion' + Undo button (rgba(255,255,255,0.22) bg pill).

**Zoomed state:** 'Reset both zooms' button top-right. rgba(0,0,0,0.5)+blur, pill. Disappears when both zoom = 1x.

**Long-press compare:** HUD hidden. Pinned photo full-screen. Accent pill banner top-center.

**Loading:** 46dp spinning ring, 3dp stroke, rgba(255,255,255,0.2) track + accent arc. Centered.

**Toast:** rgba(20,20,24,0.94) bg, white, r=14, border=line. Dismiss after 1800ms.

### 4. Delete Confirmation (Bottom Sheet)

- Scrim rgba(0,0,0,0.55), sheet bgElev bg, r=28, padding 24dp
- 52dp danger icon circle (dangerSoft bg)
- Title 'Delete N photos?' Instrument Serif italic 30sp
- Body 13sp fgDim — Trash behavior explained
- Summary card (bgElev2 bg, line border, r=16, p=14): 'N on device only' and 'N on device + Immich' rows
- Buttons: Cancel (flex 1, bgElev2) | 'Move to Trash' (flex 1.3, danger bg)
- Dry run: no deletion; show toast 'Dry run mode enabled. No photos were deleted'

### 5. Settings

**Section 1 — Source:** Radio card group. Active card: accentSoft bg + accent border. Cards: Local / Immich / Local+Immich (recommended badge). Each: icon circle + title + subtitle + radio dot.

**Section 2 — Sources/Connection** (adapts by mode):
- Local or Hybrid: Library folder row (IFolder, path, 'Change' action)
- Always: Immich URL row, API key row (masked), connection status, Test connection button

**Section 3 — Culling:**
- Long-press threshold slider (80–800ms, default 220). Accent badge shows value.
- Dry run toggle
- Mirror deletes to Immich toggle (Hybrid only)

**Section 4 — Appearance:**
- Dark theme toggle
- Accent hue: 5 swatches (hues 40/25/150/220/300), 44dp height, r=14. Active: ring shadow. Checkmark on active.

Settings card style: bgElev bg, line border, r=22. Dividers: 1dp line, 16dp horizontal margin.

### 6. Wizard *(new — not in the original design bundle)*

The Wizard reuses the app's existing token system (colors, type, radii, spacing) rather than introducing new visual language. Two screens: **Wizard Home** (coverage map) and **Wizard Month** (per-segment detail).

#### 6.1 Wizard Home

**App bar:** back circle (44dp) + title 'Wizard' (Instrument Serif italic 28sp) + subtitle '{N} months · {SourceMode}' (12sp fgDim), matching the Library app-bar pattern.

**In Progress section** (hidden if no segments are In Progress):
- Section header: 11sp 700 uppercase, 1.6sp letterSpacing, fgDim ('IN PROGRESS').
- Horizontally scrollable row of cards, sorted by most-recently-accessed. Card: bgElev2 bg, lineStrong border, r=22 (matches Continue-pill card treatment), padding 16dp. Shows month/year (Geist 700, 14sp), photo count (12sp fgDim), tap target opens the segment.

**Coverage map grid:**
- Legend row above the grid, shown once: three swatch+label pairs (Complete / In Progress / Not Started), 11sp fgDim labels.
- Grouped by year, newest year first, section header per year (11sp 700 uppercase fgDim, e.g. '2026').
- 3-column `LazyVerticalGrid`, 8dp gap, newest month first within each year.
- Month cell: r=12, aspect ~1:1, padding 10dp. Contents: month abbreviation top (Geist 700, 13sp), photo count bottom (Geist 600, 11sp, tabular-nums).
  - **Complete:** filled accent bg, dark/contrasting text.
  - **In Progress:** bgElev2 bg + accent border (lineStrong-weight, 1.5dp), fg text.
  - **Not Started:** bgElev2 bg at reduced opacity (~0.5, "near-invisible"), fgFaint text, no border.
- Empty state (no In Progress, no Complete segments): In Progress section hidden; guidance text shown above the grid — 'Tap any month to start culling.' (14sp fgDim, centered).
- Background rescan indicator: small spinner (accent, 16dp) near the app-bar subtitle while the library is being re-scanned for new photos; the grid renders from cache immediately rather than blocking on the scan.

#### 6.2 Wizard Month

**App bar:** back circle (44dp) + title (month/year, Instrument Serif italic 28sp) + a `MarkDonePill`/`DoneLabel` chip (see `ui/component/MarkDonePill.kt`) reflecting current state:
- In Progress: 'Mark done' pill, accent-outlined, tappable.
- Complete: 'Done' label chip, accentSoft bg, non-interactive (no "Mark as Complete" affordance — Complete is terminal, per `design/wizard-prd-v1.md` §7.6).

**Stats row:** three stat tiles (Photos / Marked / Deleted), bgElev card, r=16, each showing a numeral (Geist 700/800, tabular-nums) + 10sp uppercase label (fgDim).

**Stack list:** reuses `StackCard` (`ui/component/StackCard.kt`) — the same stack thumbnail/card component as the Stacks feature — in a vertical list. Entering a segment always lands at the top of this list (no sub-stack bookmarking, per PRD §7.4).

**Mark-as-complete confirmation:** bottom sheet, same visual treatment as the Delete Confirmation sheet (§4) — scrim rgba(0,0,0,0.55), sheet bgElev bg, r=28, padding 24dp. Title 'Mark {Month Year} as complete?' (Instrument Serif italic, 24–28sp). Body copy: "You can still cull photos from this period via the Library." (13sp fgDim). Buttons: Cancel (bgElev2) | Confirm (accent bg). Does **not** surface an unreviewed-photo count (per PRD §7.6 / OQ-5).

---

## State to Persist (DataStore)

- Source mode
- Library folder URI (SAF persistable permission)
- Immich URL + API key
- Long-press threshold (ms)
- Dry run toggle
- Dark theme + accent hue
- Last viewed photo index + folder URI (for Continue pill)
- Marked-for-deletion IDs (per session)
- Pinned photo ID (per session)
- Grouping window (minutes) — stack time-window, shared by Stacks and Wizard

## State to Persist (SQLite — not DataStore)

- Local↔Immich asset-ID mapping (`ImmichAssetMappingDb`) — keyed collection, not a flat setting.
- Wizard segment state (`WizardSegmentDb`): per-month `state` (NOT_STARTED/IN_PROGRESS/COMPLETE), `photoCount`, `deletedCount`, `startedAt`/`completedAt`/`lastAccessedAt`. Global across source modes (not reset by switching Local/Immich/Hybrid).

---

## Android Implementation Notes

**Photo access:**
- Local: SAF folder picker (ACTION_OPEN_DOCUMENT_TREE) on first Library open. Store with takePersistableUriPermission. Load via DocumentFile/ContentResolver. Coil for thumbnails + full-res.
- Immich: REST API, API key in header. GET /api/assets for list, GET /api/assets/{id}/original for full-res. Docs: https://immich.app/docs/api

**Deletion — MUST use Trash:**
- MediaStore.createTrashRequest() (API 30+) for device files. Never hard-delete.
- Immich: DELETE /api/assets with { ids: [...] }. Dry run: skip call, show toast.

**Long-press gesture:**
- Compose: pointerInput + detectTapGestures(onLongPress = ...).
- Suppress system context menu: Modifier.pointerInteropFilter { true } for long-press events.
- Configurable threshold: pass value to gesture detector via ViewConfiguration override.

**Zoom sync:**
- Track scale + offsetX + offsetY in ViewModel for current photo.
- On each transform change, write same values to pinnedTransform state.
- Long-press renders pinned image with its stored transform.

**Gesture conflicts:**
- When zoomed (scale > 1), disable horizontal swipe navigation.
- Vertical swipe for mark-for-deletion: use nestedScroll + consuming scroll to prevent system back swipe conflict.

**Wizard segmentation:**
- Segments are calendar months, computed from EXIF date-taken with a fallback to file last-modified time (photos with no resolvable date are excluded from the Wizard in v1 — see `design/wizard-prd-v1.md` §4/§7.1).
- Stacks within a Wizard month segment are generated using the same `PhotoGrouping.groupPhotos()` time-window algorithm as the Stacks feature, computed on entry into the segment.

---

## Assets

- **Instrument Serif** — Google Fonts; not yet bundled (see Implementation Status). Add to `res/font/` if pixel-accurate type is required.
- **Geist** — Google Fonts; not yet bundled (see Implementation Status). Add to `res/font/` if pixel-accurate type is required.
- **Icons** — Design bundle defines all icons as 24×24 SVG paths in `components/icons.jsx`, intended to be recreated as Compose `ImageVector`s. Current `CullIcons.kt` uses Material default icons instead (see Implementation Status).
- **App icon** — 5 variants described in the design bundle's canvas 'App icon' section; adaptive icon assets exist under the standard Android mipmap set. v1 ('Cull>>') is default; v4 ('C>>' monogram) intended for small sizes.

---

## Source of Truth

For pixel-level reference beyond this document, the original hand-off bundle is at `design/DESIGN_SYSTEM.md` and its accompanying files:

| File | Purpose |
|---|---|
| `design/original-prd.md` | Original product requirements (Library/Viewer core) |
| `design/wizard-prd-v1.md` | Wizard feature product requirements |
| `design/DESIGN_SYSTEM.md` | Original hand-off doc (React/HTML prototypes, pre-Wizard) |

When this file and the original hand-off disagree on a token or spec that both cover, **this file wins** — it reflects the current, agreed state of the app. Update this file (not the original bundle) whenever a design or styling change ships.
