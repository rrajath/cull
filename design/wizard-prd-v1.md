# PRD: Photo Culling Wizard — v1

**Status:** Final — ready for implementation  
**Last Updated:** June 2026  
**Scope:** v1 (Wizard feature only — Library view is out of scope)

---

## 1. Overview

The Wizard is a structured, session-based culling workflow that helps users systematically work through their entire photo library across multiple sessions. It provides visibility into progress (what has been culled) and gaps (what hasn't), so users never lose track of where they are or repeat work they've already done.

---

## 2. Problem Statement

Users have years of photos on their device — often thousands of files — that they cannot cull in a single session. Without structure:

- It's unclear where to start.
- It's easy to lose track of what has already been reviewed.
- Users re-visit the same date ranges repeatedly without making progress.
- The scale of the library is overwhelming, leading to avoidance.

The Wizard solves this by breaking the library into manageable, trackable segments and letting users pick up exactly where they left off.

---

## 3. Goals

- Give users a clear, systematic path through their entire photo library.
- Show at a glance what has been culled and what hasn't.
- Make resuming a previous session frictionless.
- Remove the cognitive load of deciding "where do I start?"

---

## 4. Non-Goals (Out of Scope for v1)

- **AI/smart suggestions** for which segment to cull next (v2).
- **Stats accuracy after completion** — segment stats are a snapshot; deletions done later via Library are not tracked back to the segment.
- **Sub-month granularity tracking** — the bookmark handles position within a segment; the segment itself is month-level.
- **Handling new photos added to a completed segment** — once marked complete, the segment stays complete regardless of new photos arriving. Ad-hoc culling via Library handles those.
- **Automatic completion detection** — completion is always an explicit user action.
- **Custom segment granularity** — segments are calendar months only in v1.
- **Unknown Date segment (v2)** — photos with no resolvable EXIF date or last-modified fallback are excluded from the Wizard in v1. A dedicated "Unknown Date" segment at the bottom of the coverage map is a v2 feature.
- **Per-source completion state** — completion states are global, not tied to a specific data source (Local/Immich/Hybrid). Switching modes does not reset or invalidate existing segment states.

---

## 5. Key Design Decisions

| Decision | Resolution |
|---|---|
| What does "Complete" mean? | An explicit user declaration of intent — not mathematical proof of full coverage. |
| Who decides when a segment is done? | The user, via a deliberate "Mark as Complete" action. |
| What if the user hasn't reviewed every photo in a segment? | They can still mark it complete. The app doesn't enforce 100% coverage. |
| What if the user wants to re-visit a completed segment? | They can use the Library view directly. The Wizard segment stays marked complete. |
| What happens to a complete segment if new photos arrive in that date range? | Nothing — the segment stays complete. Library handles ad-hoc culling. |
| Does stats accuracy matter after completion? | No. Stats are informational only and not expected to stay in sync. |
| Coverage map sort order | Newest-first (most recent months at top). |
| Can multiple segments be In Progress simultaneously? | Yes. Multiple In Progress segments are allowed. All are surfaced in the "In Progress" section at the top of the Wizard home. |
| EXIF date fallback for undated photos | Fall back to the file's last modified time. |
| Stack grouping in the Wizard | Stacks are auto-generated using the same time-window algorithm as the Library, with the time window value pulled from Settings. Stacks are computed on entry into a month segment. |
| Where does the user land when entering a segment? | Always the top of the stacks list, regardless of prior position. |
| Does the current (incomplete) calendar month appear in the grid? | Yes. The current month is shown in the grid like any other month, with whatever photos exist so far. |
| Should the "Mark as Complete" confirmation surface unreviewed photo count? | No. |

---

## 6. User Stories

**US-01 — Overview**  
As a user, I want to open the Wizard and immediately see a visual overview of my entire photo library by month, so I can understand what I've culled and what I haven't.

**US-02 — Start a new segment**  
As a user, I want to tap on an unstarted month segment and begin culling photos from that period, so I can work through my library systematically.

**US-03 — Resume an in-progress segment**  
As a user, I want to open the Wizard and be taken directly back to where I left off in my last session, so I don't have to hunt for my place.

**US-04 — Mark a segment complete**  
As a user, I want to explicitly mark a month segment as complete when I feel I've done enough culling on it, so the app knows not to surface it as unfinished work.

**US-05 — See segment stats**  
As a user, I want to see basic stats for each segment (total photos, marked for deletion, deleted) so I have a sense of the work involved before I start.

**US-06 — Identify gaps**  
As a user, I want the coverage map to clearly show which months have not been started, so I can see gaps in my culling history without having to mentally track it myself.

---

## 7. Feature Requirements

### 7.1 Auto-Segmentation by Calendar Month

- On first Wizard launch, the app scans the photo library and groups photos by calendar month (based on EXIF date taken, falling back to the file's last modified time).
- Segments are generated automatically — no user configuration required.
- Months with zero photos are not shown.
- Segments are sorted newest-first (most recent month at the top of the coverage map).
- The current calendar month is always shown, even if it has an incomplete set of photos.
- Photos with no resolvable date after the fallback chain are out of scope for v1 — see Non-Goals.

### 7.2 Coverage Map (Wizard Home Screen)

- The Wizard home screen has two sections: an **"In Progress" section** at the top, followed by the **coverage map grid** below.
- **In Progress section:** Shows a card for each segment currently In Progress, sorted by most recently accessed. Each card displays the month/year, photo count, and a tap target to enter the segment. This section is hidden if no segments are In Progress.
- **Coverage map grid:** A scrollable calendar-style grid, grouped by year (newest year first), with months laid out in a 3-column grid (newest month first within each year). Each month cell displays:
  - Month abbreviation (e.g., "Jun")
  - Photo count for that month
  - Visual state: filled blue (Complete), amber border (In Progress), near-invisible dark (Not Started)
- A legend (Complete / In Progress / Not Started) is shown once above the grid.
- Tapping any month cell in the grid — regardless of state — opens the month segment view.
- **Wizard home subtitle** displays the active data source mode, consistent with the Library screen pattern (e.g., "18 months · Hybrid").
- **First launch / all Not Started state:** When no segments are In Progress and no segments are Complete, the In Progress section is hidden and a short guidance prompt is shown above the grid: *"Tap any month to start culling."*

### 7.3 Segment States

Each segment has exactly one of the following states:

| State | Meaning |
|---|---|
| **Not Started** | User has never opened this segment in the Wizard. |
| **In Progress** | User has opened this segment and begun culling but has not marked it complete. |
| **Complete** | User has explicitly marked this segment as done. |

State transitions:
- `Not Started` → `In Progress`: User taps a Not Started segment and enters it.
- `In Progress` → `Complete`: User taps "Mark as Complete" within an In Progress segment.
- `Complete` is a terminal state in the Wizard. There is no "un-complete" in v1.

### 7.4 Resume Bookmark

- When a user exits an In Progress segment, the segment retains its In Progress state and remains visible in the "In Progress" section on the Wizard home.
- Re-entering any segment (In Progress or otherwise) always opens at the **top of the stacks list** for that month. There is no sub-stack bookmarking.
- Multiple segments can be In Progress simultaneously.

### 7.5 Start / Continue Flow

- **Start (Not Started → In Progress):** Tapping a Not Started month transitions it to In Progress and opens the month segment view. Stacks are auto-generated immediately on entry using the time-window grouping algorithm from Settings. The user lands at the top of the stacks list.
- **Continue (In Progress):** Tapping an In Progress segment from either the "In Progress" section or the coverage map grid opens the month segment view. The user lands at the top of the stacks list.
- **Revisit (Complete):** Tapping a Complete segment opens the same month segment view with the same stacks list. The user can freely enter any stack and cull photos. The segment state stays Complete — re-entering a complete segment does not revert it to In Progress. The "Mark as Complete" action is not shown (the segment is already complete).

### 7.6 Mark as Complete

- A "Mark as Complete" action is accessible from within an In Progress segment.
- Tapping it shows a brief confirmation (e.g., "Mark January 2025 as complete? You can still cull photos from this period via the Library.").
- On confirmation, the segment transitions to Complete and the user is returned to the Wizard home screen.
- The Complete state is permanent in the Wizard. Users who want to re-cull a completed segment use the Library view directly.

### 7.7 Segment Stats

Stats are displayed per segment on the coverage map and/or within the segment detail view:

| Stat | Description |
|---|---|
| Total photos | Count of photos in this month. |
| Marked for deletion | Count of photos currently flagged for deletion. |
| Deleted | Count of photos deleted during this segment's sessions. |

- Stats are informational only. They are not expected to stay perfectly in sync after the segment is marked complete.
- Stats do not gate or block any user action.

---

## 8. Data Model (Conceptual)

```
WizardSegment {
  id:             String          // e.g., "2025-01"
  label:          String          // e.g., "January 2025"
  state:          Enum            // NOT_STARTED | IN_PROGRESS | COMPLETE
  photoCount:     Int             // total photos in this month
  markedCount:    Int             // photos currently marked for deletion
  deletedCount:   Int             // photos deleted during Wizard sessions
  bookmarkIndex:  Int?            // last stack/photo index (null if not started)
  startedAt:      DateTime?       // when first entered
  completedAt:    DateTime?       // when marked complete
}
```

---

## 9. Resolved Questions

All open questions from the initial draft have been resolved. See Section 5 (Key Design Decisions) for the full record.

| # | Question | Resolution |
|---|---|---|
| OQ-1 | Sort order on coverage map | Newest-first |
| OQ-2 | Multiple segments In Progress simultaneously? | Yes, allowed |
| OQ-3 | Photos with no valid EXIF date | Fall back to file's last modified time; remaining unknowns excluded in v1 (v2 feature) |
| OQ-4 | Wizard stack grouping | Reuses Library's time-window algorithm from Settings; stacks auto-generated on month entry |
| OQ-5 | Mention unreviewed count in Mark Complete confirmation? | No |
| OQ-6 | Does switching data source mode (Local/Immich/Hybrid) affect segment states? | No — completion states are global, not per-source |
| UX-1 | Stack generation on first entry | Auto-generate immediately on entry, no confirmation step |
| UX-2 | Landing position when entering a segment | Always top of the stacks list |
| UX-3 | Show current (incomplete) month in grid? | Yes |
| UX-4 | Tapping a Complete segment | Opens the same month view as In Progress; user can still cull; state stays Complete; "Mark as Complete" action hidden |
| UX-5 | First launch / all Not Started state | Show guidance prompt: "Tap any month to start culling" |
| UX-6 | Unknown Date segment | Deferred to v2 |

---

## 10. Out of Scope — Related Features

| Feature | Where it lives |
|---|---|
| Ad-hoc photo culling | Library view (existing) |
| Bulk delete | Library view (existing) |
| Stack creation and management | Library view / existing Stack feature |
| Suggested next segment (smart recommendations) | Wizard v2 |
