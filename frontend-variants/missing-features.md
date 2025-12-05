# Missing Frontend Features

This document outlines the UI/UX features missing from the **manuscripta-teacher-portal-prototype** that should be present in an interactive prototype. 

> [!NOTE]
> This list excludes backend-dependent features (networking, actual LLM integration, database persistence, etc.) and focuses purely on frontend elements that can be demonstrated with mock data.

---

## Lesson Materials (MAT)

### MAT8 — Source Document Upload in Content Creator
**Current:** File upload only exists in the Unit Creator (`LessonCreator.tsx`).  
**Missing:** The `ContentCreatorModal.tsx` should also allow uploading source documents (.pdf, .txt, .docx) when generating individual content items.

---

### MAT15 — Import and Display Static Images and PDFs
**Current:** No image/PDF import or display capability.  
**Missing:** 
- UI to import images and PDF files as lesson materials
- Preview/display component for these file types within the content editor

---

### MAT16 — Vocabulary/Keywords Editor
**Current:** Not implemented.  
**Missing:**
- UI to highlight and define keywords/vocabulary terms for each material
- Could be a sidebar panel or inline editor in `ContentEditorModal.tsx`
- Display of vocabulary terms when viewing a material

---

### MAT17 — Reading Age Level Slider
**Current:** Age group slider exists (4-6, 7-9, 10-13, 14+).  
**Missing:** A **supplementary slider** or input for specifying a target Progressive Skills reading age level (e.g., "reading age of 8 years"), separate from the general age group.

---

### MAT18 — Response Marking Interface
**Current:** No marking/grading UI.  
**Missing:**
- Interface to view student responses (can use mock data)
- Manual marking controls (correct/incorrect buttons)
- AI-assisted marking toggle (UI only, mock functionality)

---

### MAT19 — Points System Configuration
**Current:** Not implemented.  
**Missing:**
- Toggle to enable/disable point system for a material
- UI to configure points per correct answer
- Display of points in quiz/worksheet preview

---

## Classroom Control (CON)

### CON5 — Battery Level Display
**Current:** Alerts panel exists but doesn't show battery info.  
**Missing:** Battery level indicator for each device in the tablet grid (can use random mock values).

---

### CON10 — Differentiation / Subgroup Assignment
**Current:** Materials launch to all devices.  
**Missing:**
- UI to create/manage device subgroups (e.g., "High ability", "Support group")
- Ability to select a subgroup before launching a material
- Visual indication of which group each tablet belongs to

---

### CON11 — Export Session Data
**Current:** Not implemented.  
**Missing:**
- "Export" button in the dashboard
- Modal to select export format (CSV, JSON)
- Mock download action (can generate a sample file)

---

### CON12 — Explicit Help Request Alert
**Current:** "needs_help" status shows in the grid but no prominent alert.  
**Missing:**
- Toast notification or alert banner when a student raises their hand
- Blinking/pulsing animation on the affected tablet card
- Sound alert option (toggle in settings)

---

### CON13 — Live View Preview
**Current:** Not implemented.  
**Missing:**
- "View Screen" button on each tablet card
- Modal showing a mock preview of the student's current view
- Can display a static screenshot or the current material they're viewing

---

### CON14 — Split Dashboard (Public/Private Tabs)
**Current:** Single dashboard view.  
**Missing:**
- Two dashboard modes: **Presentation Mode** (safe for projector) and **Teacher Mode** (full details)
- Tab or toggle to switch between modes
- Presentation mode should hide sensitive info (e.g., individual student struggles, detailed alerts)

---

## Content Types

### Missing Material Types
**Current:** `Lesson`, `Worksheet`, `Quiz`  
**Missing:**
- `Poll` — Quick voting material type
- `Reading` — Read-only informational content

---

## Settings / Accessibility

### ACC3A — Per-Tablet Accessibility Toggles
**Current:** Global settings only.  
**Missing:**
- In the tablet detail view or a device settings modal:
  - Text-to-speech toggle
  - AI summary toggle
  - Animated avatar toggle
- These should be configurable per-device or per-group

---

## UI States

### Loading States / Throbbers
**Current:** No loading indicators.  
**Missing:**
- Loading spinner when "generating" content (simulated delay)
- Skeleton loaders when navigating between views
- Progress indicator for mock AI generation

---

### Empty States
**Current:** Basic empty state messages exist.  
**Missing:** More engaging empty states with illustrations or calls-to-action, especially for:
- Empty units
- No search results
- No deployed materials

---

## Data Model Alignment

### Type Definitions
**Current `types.ts`** doesn't match the spec's Validation Rules.  
**Recommended additions:**

```typescript
// Content type enum alignment
export type MaterialType = 'READING' | 'WORKSHEET' | 'POLL' | 'QUIZ' | 'LESSON';

// Vocabulary support
export interface VocabularyTerm {
  term: string;
  definition: string;
}

// Question types for quizzes
export type QuestionType = 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'WRITTEN_ANSWER';

// Device status alignment
export type DeviceStatus = 'ON_TASK' | 'IDLE' | 'HAND_RAISED' | 'LOCKED' | 'DISCONNECTED';

// Session status for tracking
export type SessionStatus = 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'CANCELLED';

// Extended ContentItem with vocabulary
export interface ContentItem {
  // ... existing fields ...
  vocabularyTerms?: VocabularyTerm[];
  pointsEnabled?: boolean;
  pointsPerQuestion?: number;
}
```

---

## Priority Checklist

### High Priority (Core UX)
- [ ] Add vocabulary/keywords editor (MAT16)
- [ ] Implement split dashboard modes (CON14)
- [ ] Add subgroup assignment UI (CON10)
- [ ] Add `POLL` and `READING` content types

### Medium Priority (Enhanced Experience)
- [ ] Add reading age level slider (MAT17)
- [ ] Add battery level to device cards (CON5)
- [ ] Add help request alerts with animation (CON12)
- [ ] Add export session data button (CON11)
- [ ] Implement loading states/throbbers

### Lower Priority (Polish)
- [ ] Add source upload to content creator (MAT8)
- [ ] Add image/PDF import and display (MAT15)
- [ ] Add response marking interface (MAT18)
- [ ] Add points system config (MAT19)
- [ ] Add live view mock preview (CON13)
- [ ] Add per-tablet accessibility settings (ACC3A)

---

*Last updated: December 2024*
