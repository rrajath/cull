# Handoff: CULL — Android Photo Culling App

## Overview

CULL is an Android photo-culling app. Core interaction: pin a reference image, long-press any other image to compare, swipe down to mark for deletion. Supports local folder culling and Immich self-hosted integration.


---

## About the Design Files

Files in this bundle are **high-fidelity HTML/React prototypes** — design references, not production code. Recreate in **native Android (Kotlin + Jetpack Compose)**. Open `CULL - Android photo culler.html` in a browser for the full pannable canvas; Section 05 is a live touch prototype.

**Fidelity: High.** Match colors, type, spacing, radii, and gestures precisely.


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


> **Accent hue** is user-configurable. Default = 40 (tangerine). Options: 40, 25, 150, 220, 300. Store in DataStore; rebuild MaterialTheme on change. Convert oklch to sRGB at https://oklch.com.


### Typography

| Role | Font | Weight | Size | Notes |
|---|---|---|---|---|
| Display/hero | Instrument Serif | 400 italic | 44-88sp | letterSpacing -1 to -2 |
| Screen title | Instrument Serif | 400 italic | 28-32sp | letterSpacing -0.6 |
| UI body | Geist | 400 | 13-15sp | |
| UI label | Geist | 600-700 | 12-14sp | letterSpacing 0.2 |
| Section header | Geist | 700 | 11sp | 1.6sp, uppercase |
| Numerics | Geist | 600-800 | varies | tabular-nums |


Both fonts on Google Fonts. Add to res/font/.


### Radii

| Usage | dp |
|---|---|
| Screen corner | 42 |
| Giant home buttons | 32 |
| Settings cards | 22 |
| Thumbnails | 14 |
| Toasts | 14 |
| Pill/circle | 999 |


### Spacing
Base = 4dp. Scale: 4, 8, 12, 16, 20, 24, 28, 32, 40, 48.


---

## Source Mode

App-wide, stored in DataStore. Chosen on Settings > Source.

| Mode | Description |
|---|---|
| local | Device folder only. Immich not used. Cloud chip hidden in HUD. |
| immich | Cull Immich library directly. No local files. Mirror toggle hidden. |
| hybrid *(default)* | Device folder + mirror deletions to Immich. Cloud chip shown per image. |


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
  - Albums: bgElev bg + line border, r=32, minHeight 118, opacity 0.55.
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


**Index counter:** top-center absolute. Format '0142  .  1,208'. rgba(0,0,0,0.55)+blur, pill, 12sp 600.

**HUD closed pill:** bottom-center. Accent count badge + 'Tap for HUD'. rgba(16,16,18,0.76)+blur, pill.

**HUD open (pill variant — default):** Two rows, bottom 18dp:
1. Cloud status chip (NON-INTERACTIVE, pointerEvents=none): rgba(0,0,0,0.42)+blur, pill, 10.5sp uppercase. 'On Immich' (ICloud) or 'Local only' (ICloudOff, dimmer). Hidden if source=local.
2. Action strip: rgba(16,16,18,0.86)+blur, pill, 6dp padding. Left: 28dp danger circle + '{N} marked'. Right: pin button (40dp, pin bg when active) + Confirm (accent bg, pill).

**HUD variants:** Rail (right-side vertical pill, vertically centered) and Bar (full-width bottom gradient) — see viewer.jsx and the canvas Section 03.

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
- Long-press threshold slider (80-800ms, default 220). Accent badge shows value.
- Dry run toggle
- Mirror deletes to Immich toggle (Hybrid only)

**Section 4 — Appearance:**
- Dark theme toggle
- Accent hue: 5 swatches (hues 40/25/150/220/300), 44dp height, r=14. Active: ring shadow. Checkmark on active.

Settings card style: bgElev bg, line border, r=22. Dividers: 1dp line, 16dp horizontal margin.


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


---

## Assets

- **Instrument Serif** — Google Fonts, add to res/font/
- **Geist** — Google Fonts, add to res/font/
- **Icons** — All icons in components/icons.jsx as 24x24 SVG paths. Recreate as Compose ImageVector.
- **App icon** — 5 variants in the canvas 'App icon' section. v1 (Cull>>) is default; v4 (C>> monogram) for small sizes. Adaptive icon: bg #1a1612, foreground glyph.


---

## Files in This Bundle

| File | Purpose |
|---|---|
| CULL - Android photo culler.html | Full interactive canvas. Open in browser. |
| components/theme.jsx | All design tokens. Source of truth. |
| components/icons.jsx | All icon SVG paths (24x24). |
| screens/home.jsx | Home screen |
| screens/library.jsx | Library grid |
| screens/viewer.jsx | Viewer + HUD variants + all states |
| screens/settings.jsx | Settings screen |
| screens/live-viewer.jsx | Interactive prototype (gesture reference) |
| original-prd.md | Original product requirements document |


---

## Recommended Issue Order for Claude Code

1. Design tokens (Theme.kt, Type.kt, Color.kt)
2. App icon (adaptive icon, all 4 variants)
3. Home screen (static)
4. Source mode switcher + DataStore persistence
5. Library grid (Coil thumbnails, marked/pinned overlays)
6. Viewer shell (full-screen photo, index counter, swipe navigation)
7. HUD pill variant
8. Pin gesture + pin badge + persistence
9. Long-press compare (configurable threshold, gesture suppression)
10. Swipe-down mark/unmark + greyscale animation
11. Pinch-to-zoom + zoom sync to pinned
12. Reset-both-zooms button
13. Settings screen (source radio, folder picker, Immich config, slider, toggles, accent hue)
14. Immich client (list assets, load full-res, cloud chip)
15. Delete confirmation dialog (Trash API, Immich delete, dry run)
16. Continue-where-you-left-off (session persistence)
17. Light theme
18. HUD rail + bar variants
