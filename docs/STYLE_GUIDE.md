# Comprehensive Style Guide

> **Version:** X.Y.Z • Academic Modern  
> **Last Updated:** January 2026  
> **Framework:** React + TypeScript + Tailwind CSS + Vite

---

## Table of Contents

1. [Design Philosophy](#design-philosophy)
2. [Color System](#color-system)
3. [Typography](#typography)
4. [Fraunces Variable Font](#fraunces-variable-font)
5. [Header Settings](#header-settings)
6. [Font Settings Summary](#font-settings-summary)
7. [Spacing System](#spacing-system)
8. [Border Radius](#border-radius)
9. [Shadows & Elevation](#shadows--elevation)
10. [Layout System](#layout-system)
11. [Component Patterns](#component-patterns)
12. [Interactive States](#interactive-states)
13. [Icons & Imagery](#icons--imagery)
14. [Animation & Transitions](#animation--transitions)
15. [Z-Index Scale](#z-index-scale)
16. [Responsive Design](#responsive-design)
17. [Dark Mode Considerations](#dark-mode-considerations)
18. [Accessibility](#accessibility)
19. [Code Examples](#code-examples)

---

## Design Philosophy

This style guide describes an **Academic Modern** design approach:

- **Clean & Professional**: Minimal visual clutter with clear hierarchy
- **Functional First**: Every element serves a purpose
- **Consistent**: Repeated patterns create familiarity
- **Accessible**: High contrast, clear focus states, screen reader friendly
- **Responsive**: Works seamlessly across devices

### Core Principles

1. **Clarity over decoration** - Use whitespace generously
2. **Consistency over creativity** - Reuse established patterns
3. **Feedback over ambiguity** - Show state changes clearly
4. **Hierarchy through size and color** - Guide the eye naturally

---

## Color System

### Brand Colors

| Color Name | Tailwind Class | Hex Value | Usage |
|------------|----------------|-----------|-------|
| Brand Green | `bg-brand-green` | `#2D5A3D` | Primary sidebar, headers, CTAs |
| Brand Orange | `bg-brand-orange` | `#E07A3D` | Accents, active states, highlights |

### Semantic Colors

```css
/* Primary Palette */
--color-brand-green: #2D5A3D;      /* Primary brand color */
--color-brand-green-dark: #1E3D2A; /* Hover/pressed states */
--color-brand-green-light: #3D7A52; /* Lighter variant */

--color-brand-orange: #E07A3D;     /* Accent/highlight color */
--color-brand-orange-dark: #C56A30; /* Hover/pressed states */
--color-brand-orange-light: #F09A5D; /* Lighter variant */

/* Neutrals */
--color-white: #FFFFFF;
--color-gray-50: #F9FAFB;
--color-gray-100: #F3F4F6;
--color-gray-200: #E5E7EB;
--color-gray-300: #D1D5DB;
--color-gray-400: #9CA3AF;
--color-gray-500: #6B7280;
--color-gray-600: #4B5563;
--color-gray-700: #374151;
--color-gray-800: #1F2937;
--color-gray-900: #111827;

/* Status Colors */
--color-success: #10B981;          /* Green - success states */
--color-warning: #F59E0B;          /* Amber - warning states */
--color-error: #EF4444;            /* Red - error states */
--color-info: #3B82F6;             /* Blue - info states */

/* Background Colors */
--color-bg-primary: #F9FAFB;       /* Main content background */
--color-bg-secondary: #FFFFFF;     /* Card backgrounds */
--color-bg-sidebar: #2D5A3D;       /* Sidebar background */
```

### Color Usage Guidelines

| Context | Color | Class |
|---------|-------|-------|
| Sidebar background | Brand Green | `bg-brand-green` |
| Active navigation | Brand Orange | `bg-brand-orange` |
| Primary buttons | Brand Green | `bg-brand-green hover:bg-brand-green-dark` |
| Secondary buttons | Gray | `bg-gray-100 hover:bg-gray-200` |
| Page background | Gray 50 | `bg-gray-50` |
| Card background | White | `bg-white` |
| Primary text | Gray 900 | `text-gray-900` |
| Secondary text | Gray 600 | `text-gray-600` |
| Muted text | Gray 400 | `text-gray-400` |
| Links | Brand Green | `text-brand-green hover:text-brand-green-dark` |
| Borders | Gray 200 | `border-gray-200` |
| Dividers | Gray 100 | `border-gray-100` |

### Opacity Variants

For overlays and subtle backgrounds:

```
white/10  - 10% white (hover backgrounds on dark surfaces)
white/20  - 20% white (slightly more visible)
white/50  - 50% white (muted text on dark)
black/10  - 10% black (subtle shadows)
black/50  - 50% black (modal backdrops)
```

---

## Typography

### Font Families

```css
/* Primary Font Stack (Sans-Serif) - Body text, UI elements */
font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 
             Roboto, 'Helvetica Neue', Arial, sans-serif;

/* Display/Header Font Stack (Serif) - Branding, major headings */
font-family: 'Fraunces', 'Georgia', 'Times New Roman', serif;

/* Monospace (for code) */
font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
```

### Font Loading (index.html)

Add these Google Fonts imports to your `<head>`:

```html
<!-- Preconnect for performance -->
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>

<!-- Inter - Primary UI font -->
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">

<!-- Fraunces - Header/Brand font (Variable font with optical size) -->
<link href="https://fonts.googleapis.com/css2?family=Fraunces:ital,opsz,wght@0,9..144,100..900;1,9..144,100..900&display=swap" rel="stylesheet">

<!-- JetBrains Mono - Code font (optional) -->
<link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
```

### Tailwind Font Classes

| Class | Font Family | Usage |
|-------|-------------|-------|
| `font-sans` | Inter | **Default** - Body text, buttons, navigation, UI |
| `font-serif` | Fraunces | Brand name, hero headings, elegant titles |
| `font-mono` | JetBrains Mono | Code blocks, technical data |

### Fraunces Variable Font

**Fraunces** is a display typeface with a unique "wonky" character, well-suited to an Academic Modern aesthetic. It's a variable font with multiple axes:

#### Variable Font Axes

| Axis | Code | Range | Description |
|------|------|-------|-------------|
| Weight | `wght` | 100–900 | Thin to Black |
| Optical Size | `opsz` | 9–144 | Auto-adjusts details for size |
| WONK | `WONK` | 0–1 | Toggles "wonky" quirky forms |
| SOFT | `SOFT` | 0–100 | Adjusts corner softness |

#### Recommended Fraunces Settings

```css
/* Preferred preset (DEFAULT) */
.fraunces-preferred {
  font-family: 'Fraunces', serif;
  font-weight: 800;
  font-variation-settings: 'opsz' 24, 'SOFT' 0, 'WONK' 0;
}

/* Brand name / Logo - Preferred (clean, academic) */
.brand-title {
  font-family: 'Fraunces', serif;
  font-weight: 800;
  font-variation-settings: 'opsz' 24, 'SOFT' 0, 'WONK' 0;
}

/* Large display headings - Preferred (same preset, heavier weight) */
.display-heading {
  font-family: 'Fraunces', serif;
  font-weight: 800;
  font-variation-settings: 'opsz' 24, 'SOFT' 0, 'WONK' 0;
}

/* Smaller elegant headings - more refined */
.elegant-heading {
  font-family: 'Fraunces', serif;
  font-weight: 800;
  font-variation-settings: 'opsz' 24, 'SOFT' 0, 'WONK' 0;
}

/* Note: `SOFT` and `WONK` are variable axes; if your font import
   doesn't include them (or you’re using a static Fraunces), the browser
   will safely ignore those axis settings. */
```

#### Tailwind CSS Custom Classes (Optional)

Add to your CSS for easy Fraunces variants:

```css
@layer utilities {
  .font-fraunces-preferred {
    font-family: 'Fraunces', serif;
    font-weight: 800;
    font-variation-settings: 'opsz' 24, 'SOFT' 0, 'WONK' 0;
  }

  .font-fraunces-display {
    font-family: 'Fraunces', serif;
    font-weight: 800;
    font-variation-settings: 'opsz' 24, 'SOFT' 0, 'WONK' 0;
  }
  
  .font-fraunces-heading {
    font-family: 'Fraunces', serif;
    font-weight: 800;
    font-variation-settings: 'opsz' 24, 'SOFT' 0, 'WONK' 0;
  }
  
  .font-fraunces-wonky {
    font-variation-settings: 'WONK' 1;
  }
  
  .font-fraunces-soft {
    font-variation-settings: 'SOFT' 100;
  }
}
```

#### Why Fraunces

- **Academic character**: Old-style serifs with a scholarly feel
- **Personality**: The "wonky" axis adds warmth and approachability
- **Versatility**: Variable axes allow tuning for different contexts
- **Modern yet classic**: Fits the "Academic Modern" design philosophy

---

## Header Settings

### Main Application Header

```jsx
<header className="h-16 bg-white border-b border-gray-200 px-6 flex items-center justify-between">
  {/* Left: Logo/Brand */}
  <div className="flex items-center gap-3">
    <div className="w-8 h-8 bg-brand-green rounded-lg flex items-center justify-center">
      <span className="text-white font-bold text-sm">B</span>
    </div>
    <div>
      <h1
        className="font-serif text-xl font-extrabold text-gray-900 tracking-tight"
        style={{ fontVariationSettings: "'opsz' 24, 'SOFT' 0, 'WONK' 0" }}
      >
        Brandname
      </h1>
      <p className="text-xs text-gray-500 font-sans -mt-0.5">Product Dashboard</p>
    </div>
  </div>
  
  {/* Center: Page Title (optional) */}
  <div className="hidden md:block">
    <h2 className="text-lg font-semibold text-gray-800 font-sans">{pageTitle}</h2>
  </div>
  
  {/* Right: Actions */}
  <div className="flex items-center gap-4">
    {/* Search, notifications, user menu */}
  </div>
</header>
```

### Header Dimensions & Styling

| Property | Value | Tailwind Class |
|----------|-------|----------------|
| Height | 64px | `h-16` |
| Background | White | `bg-white` |
| Border | Bottom, gray-200 | `border-b border-gray-200` |
| Padding | 24px horizontal | `px-6` |
| Layout | Flex, centered, spaced | `flex items-center justify-between` |
| Z-Index | 10 (below sidebar) | `z-10` |

### Brand/Logo Typography

```jsx
// Brand Name (Serif - elegant, academic)
<h1
  className="font-serif text-xl font-extrabold text-gray-900 tracking-tight"
  style={{ fontVariationSettings: "'opsz' 24, 'SOFT' 0, 'WONK' 0" }}
>
  Brandname
</h1>

// Alternative: Larger brand for landing pages
<h1
  className="font-serif text-3xl font-extrabold text-gray-900 tracking-tight"
  style={{ fontVariationSettings: "'opsz' 24, 'SOFT' 0, 'WONK' 0" }}
>
  Brandname
</h1>
```

| Property | Value | Class |
|----------|-------|-------|
| Font Family | Fraunces | `font-serif` |
| Font Size | 20px (header), 30px (hero) | `text-xl`, `text-3xl` |
| Font Weight | 800 | `font-extrabold` |
| Color | Gray 900 | `text-gray-900` |
| Letter Spacing | Tight | `tracking-tight` |

### Subtitle/Tagline Typography

```jsx
// Subtitle under brand
<p className="text-xs text-gray-500 font-sans -mt-0.5">Product Dashboard</p>

// Tagline
<p className="text-sm text-gray-600 font-sans">Content management</p>
```

| Property | Value | Class |
|----------|-------|-------|
| Font Family | Inter | `font-sans` |
| Font Size | 12px or 14px | `text-xs`, `text-sm` |
| Font Weight | 400 | `font-normal` (default) |
| Color | Gray 500 or 600 | `text-gray-500`, `text-gray-600` |

### Page Title (In Header)

```jsx
<h2 className="text-lg font-semibold text-gray-800 font-sans">
  {pageTitle}
</h2>
```

| Property | Value | Class |
|----------|-------|-------|
| Font Family | Inter | `font-sans` |
| Font Size | 18px | `text-lg` |
| Font Weight | 600 | `font-semibold` |
| Color | Gray 800 | `text-gray-800` |

### Content Page Headers

```jsx
// Main Page Title
<div className="mb-6">
  <h1 className="text-2xl font-bold text-gray-900 font-sans">
    Dashboard
  </h1>
  <p className="text-sm text-gray-600 mt-1">
    Overview of your workspace and activity
  </p>
</div>

// With Breadcrumb
<div className="mb-6">
  <nav className="text-sm text-gray-500 mb-2">
    <span>Home</span>
    <span className="mx-2">/</span>
    <span className="text-gray-900">Library</span>
  </nav>
  <h1 className="text-2xl font-bold text-gray-900">Library</h1>
</div>
```

### Section Headers (Within Pages)

```jsx
// Card/Section Title
<h3 className="text-lg font-semibold text-gray-900 mb-4 font-sans">
  Recent Activity
</h3>

// Subsection
<h4 className="text-base font-medium text-gray-700 mb-2 font-sans">
  Settings
</h4>

// Small section label
<h5 className="text-sm font-medium text-gray-500 uppercase tracking-wide mb-2">
  Options
</h5>
```

### Complete Header Component Example

```tsx
import React from 'react';
import { Bell, Search, User, Menu } from 'lucide-react';

interface HeaderProps {
  pageTitle?: string;
  onMenuToggle?: () => void;
}

export const Header: React.FC<HeaderProps> = ({ pageTitle, onMenuToggle }) => {
  return (
    <header className="h-16 bg-white border-b border-gray-200 px-4 md:px-6 flex items-center justify-between sticky top-0 z-10">
      {/* Mobile menu button */}
      <button 
        onClick={onMenuToggle}
        className="lg:hidden p-2 rounded-md hover:bg-gray-100 transition-colors"
      >
        <Menu className="w-5 h-5 text-gray-600" />
      </button>

      {/* Brand */}
      <div className="flex items-center gap-3">
        <div className="w-8 h-8 bg-brand-green rounded-lg flex items-center justify-center shadow-sm">
          <span className="text-white font-serif font-bold text-sm">B</span>
        </div>
        <div className="hidden sm:block">
          <h1
            className="font-serif text-xl font-extrabold text-gray-900 tracking-tight leading-none"
            style={{ fontVariationSettings: "'opsz' 24, 'SOFT' 0, 'WONK' 0" }}
          >
            Brandname
          </h1>
          <p className="text-xs text-gray-500 font-sans">Product Dashboard</p>
        </div>
      </div>

      {/* Page Title - Center (desktop only) */}
      {pageTitle && (
        <div className="hidden md:block absolute left-1/2 transform -translate-x-1/2">
          <h2 className="text-lg font-semibold text-gray-800 font-sans">
            {pageTitle}
          </h2>
        </div>
      )}

      {/* Right Actions */}
      <div className="flex items-center gap-2 md:gap-4">
        {/* Search */}
        <button className="p-2 rounded-md hover:bg-gray-100 transition-colors">
          <Search className="w-5 h-5 text-gray-500" />
        </button>

        {/* Notifications */}
        <button className="p-2 rounded-md hover:bg-gray-100 transition-colors relative">
          <Bell className="w-5 h-5 text-gray-500" />
          <span className="absolute top-1 right-1 w-2 h-2 bg-brand-orange rounded-full" />
        </button>

        {/* User Avatar */}
        <button className="flex items-center gap-2 p-1.5 rounded-md hover:bg-gray-100 transition-colors">
          <div className="w-8 h-8 bg-brand-green rounded-full flex items-center justify-center">
            <User className="w-4 h-4 text-white" />
          </div>
          <span className="hidden md:block text-sm font-medium text-gray-700">
            John Doe
          </span>
        </button>
      </div>
    </header>
  );
};
```

---

## Font Settings Summary

### When to Use Each Font

| Font | Class | Use For |
|------|-------|---------|
| **Fraunces** (Serif) | `font-serif` | Brand name, hero headings, elegant/formal titles, marketing pages |
| **Inter** (Sans-Serif) | `font-sans` | Everything else - body text, buttons, navigation, labels, page titles, UI elements |
| **JetBrains Mono** | `font-mono` | Code snippets, technical IDs, timestamps, monospaced data |

### Font Pairing: Fraunces + Inter

The combination of **Fraunces** (serif) and **Inter** (sans-serif) creates a sophisticated yet modern aesthetic:

- **Fraunces**: Adds warmth, personality, and academic gravitas
- **Inter**: Provides clean, highly legible UI text

### Typography Pairing Examples

```jsx
// Brand + Tagline
<div>
  <h1 className="font-serif text-3xl font-bold">Brandname</h1>
  <p className="font-sans text-lg text-gray-600">Modern content platform</p>
</div>

// Section with elegant header
<section>
  <h2 className="font-serif text-2xl font-semibold mb-2">Featured Items</h2>
  <p className="font-sans text-sm text-gray-600 mb-6">
    Curated content for your audience
  </p>
</section>

// Standard UI heading (use sans-serif)
<div>
  <h2 className="font-sans text-xl font-bold">Project Dashboard</h2>
  <p className="font-sans text-sm text-gray-500">View your activity and key metrics</p>
</div>
```

### Font Size Scale

| Size Name | Tailwind Class | Pixel Size | Line Height | Usage |
|-----------|----------------|------------|-------------|-------|
| Extra Small | `text-xs` | 12px | 16px | Captions, helper text, badges |
| Small | `text-sm` | 14px | 20px | Body text, inputs, buttons |
| Base | `text-base` | 16px | 24px | Default body text |
| Large | `text-lg` | 18px | 28px | Subheadings |
| XL | `text-xl` | 20px | 28px | Section headers |
| 2XL | `text-2xl` | 24px | 32px | Page titles |
| 3XL | `text-3xl` | 30px | 36px | Large titles |
| 4XL | `text-4xl` | 36px | 40px | Hero text |

### Font Weights

| Weight | Tailwind Class | Numeric | Usage |
|--------|----------------|---------|-------|
| Normal | `font-normal` | 400 | Body text |
| Medium | `font-medium` | 500 | Navigation, labels |
| Semibold | `font-semibold` | 600 | Buttons, headings |
| Bold | `font-bold` | 700 | Emphasis, titles |
| Extra Bold | `font-extrabold` | 800 | Brand name (Fraunces preset) |
| Black | `font-black` | 900 | Rare: hero/marketing emphasis |

### Typography Combinations

```jsx
// Page Title
<h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>

// Section Header
<h2 className="text-xl font-semibold text-gray-800">Recent Activity</h2>

// Card Title
<h3 className="text-lg font-semibold text-gray-900">Item Overview</h3>

// Subsection
<h4 className="text-base font-medium text-gray-700">Details</h4>

// Body Text
<p className="text-sm text-gray-600">Description text goes here.</p>

// Caption/Helper
<span className="text-xs text-gray-400">Last updated 2 hours ago</span>

// Navigation Item
<span className="text-sm font-medium">Library</span>

// Button Text
<button className="text-sm font-medium">Save Changes</button>
```

### Line Height

| Tailwind Class | Value | Usage |
|----------------|-------|-------|
| `leading-none` | 1 | Tightly packed text |
| `leading-tight` | 1.25 | Headings |
| `leading-snug` | 1.375 | Cards |
| `leading-normal` | 1.5 | Default body |
| `leading-relaxed` | 1.625 | Long-form content |
| `leading-loose` | 2 | Very spaced |

---

## Spacing System

### Base Unit
The spacing system uses **4px as the base unit** following Tailwind's default scale.

### Spacing Scale

| Tailwind Class | Pixels | Rem | Common Usage |
|----------------|--------|-----|--------------|
| `1` | 4px | 0.25rem | Tight gaps, icon margins |
| `2` | 8px | 0.5rem | Small gaps, compact spacing |
| `3` | 12px | 0.75rem | Medium-small spacing |
| `4` | 16px | 1rem | Standard spacing, button padding |
| `5` | 20px | 1.25rem | Medium spacing |
| `6` | 24px | 1.5rem | Section padding, card padding |
| `8` | 32px | 2rem | Large section spacing |
| `10` | 40px | 2.5rem | Extra large spacing |
| `12` | 48px | 3rem | Page sections |
| `16` | 64px | 4rem | Major sections |

### Padding Patterns

```jsx
// Buttons
<button className="px-4 py-2">Small Button</button>
<button className="px-4 py-3">Standard Button</button>
<button className="px-6 py-3">Large Button</button>

// Cards
<div className="p-4">Compact Card</div>
<div className="p-6">Standard Card</div>
<div className="p-8">Spacious Card</div>

// Inputs
<input className="px-3 py-2" />
<input className="px-4 py-3" />

// Container/Section
<section className="p-6">Content</section>
<main className="p-8">Main Content</main>
```

### Margin & Gap Patterns

```jsx
// Vertical Stacking
<div className="space-y-2">Tight Stack (8px)</div>
<div className="space-y-4">Standard Stack (16px)</div>
<div className="space-y-6">Comfortable Stack (24px)</div>
<div className="space-y-8">Spacious Stack (32px)</div>

// Horizontal Spacing
<div className="space-x-2">Tight Horizontal (8px)</div>
<div className="space-x-4">Standard Horizontal (16px)</div>

// Grid Gaps
<div className="gap-4">Standard Grid Gap</div>
<div className="gap-6">Comfortable Grid Gap</div>
```

### Sidebar Specific

```jsx
// Sidebar container
className="w-64 p-6"  // 256px width, 24px padding

// Navigation items
className="space-y-2"  // 8px between items

// Nav button
className="px-4 py-3"  // 16px horizontal, 12px vertical
```

---

## Border Radius

### Border Radius Scale

| Tailwind Class | Pixels | Usage |
|----------------|--------|-------|
| `rounded-none` | 0px | Square corners |
| `rounded-sm` | 2px | Subtle rounding |
| `rounded` | 4px | Small elements |
| `rounded-md` | 6px | **Navigation items, buttons** |
| `rounded-lg` | 8px | Cards, modals |
| `rounded-xl` | 12px | Large cards, sections |
| `rounded-2xl` | 16px | Hero sections |
| `rounded-full` | 9999px | Pills, avatars, circles |

### Usage Guidelines

| Element | Recommended Radius | Class |
|---------|-------------------|-------|
| Navigation buttons | Medium | `rounded-md` |
| Primary buttons | Medium | `rounded-md` |
| Input fields | Medium | `rounded-md` |
| Cards | Large | `rounded-lg` |
| Modals | XL | `rounded-xl` |
| Badges/Tags | Full | `rounded-full` |
| Avatar images | Full | `rounded-full` |
| Thumbnails | Large | `rounded-lg` |
| Dropdowns | Medium | `rounded-md` |
| Tooltips | Medium | `rounded-md` |

### Examples

```jsx
// Navigation Button
<button className="rounded-md">Nav Item</button>

// Card
<div className="rounded-lg bg-white shadow">Card Content</div>

// Modal
<div className="rounded-xl bg-white shadow-xl">Modal Content</div>

// Badge
<span className="rounded-full bg-brand-green text-white px-3 py-1">
  Active
</span>

// Avatar
<img className="rounded-full w-10 h-10" src="..." alt="User" />

// Input
<input className="rounded-md border border-gray-300 px-3 py-2" />
```

---

## Shadows & Elevation

### Shadow Scale

| Tailwind Class | Usage | CSS Value |
|----------------|-------|-----------|
| `shadow-sm` | Subtle elevation | `0 1px 2px 0 rgb(0 0 0 / 0.05)` |
| `shadow` | Default cards | `0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1)` |
| `shadow-md` | **Active states** | `0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)` |
| `shadow-lg` | Dropdowns, popovers | `0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1)` |
| `shadow-xl` | **Sidebar, major panels** | `0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1)` |
| `shadow-2xl` | Modals | `0 25px 50px -12px rgb(0 0 0 / 0.25)` |

### Elevation Hierarchy

```
Level 0: No shadow      - Background surfaces
Level 1: shadow-sm      - Subtle cards, input fields (resting)
Level 2: shadow         - Default card state
Level 3: shadow-md      - Active navigation, hover states
Level 4: shadow-lg      - Dropdowns, popovers
Level 5: shadow-xl      - Sidebar, major navigation panels
Level 6: shadow-2xl     - Modals, dialogs, critical UI
```

### Usage Examples

```jsx
// Sidebar Panel (fixed, high prominence)
<aside className="shadow-xl bg-brand-green">...</aside>

// Standard Card
<div className="shadow bg-white rounded-lg">...</div>

// Card with hover effect
<div className="shadow hover:shadow-md transition-shadow">...</div>

// Active Navigation Item
<button className="shadow-md bg-brand-orange">Active Nav</button>

// Dropdown Menu
<div className="shadow-lg rounded-md bg-white">...</div>

// Modal
<div className="shadow-2xl rounded-xl bg-white">...</div>
```

---

## Layout System

### Main Layout Structure

```jsx
<div className="min-h-screen bg-gray-50">
  {/* Top-level container */}
  <div className="flex">
    {/* Sidebar - Fixed width */}
    <aside className="w-64 min-h-screen bg-brand-green shadow-xl z-20">
      {/* Sidebar content */}
    </aside>
    
    {/* Main content area - Flexible */}
    <main className="flex-1 overflow-hidden">
      {/* Header */}
      <header className="h-16 bg-white border-b border-gray-200">
        {/* Header content */}
      </header>
      
      {/* Content */}
      <div className="p-6 overflow-auto">
        {/* Page content */}
      </div>
    </main>
  </div>
</div>
```

### Sidebar Dimensions

```
Width: 256px (w-64)
Padding: 24px (p-6)
Background: brand-green
Text: white
Shadow: shadow-xl
Z-Index: z-20
```

### Header Dimensions

```
Height: 64px (h-16)
Background: white
Border: border-b border-gray-200
Padding: px-6 (24px horizontal)
```

### Content Area

```
Padding: p-6 (24px all sides)
Background: bg-gray-50 (main) or bg-white (cards)
Max-width: Use container classes or custom (max-w-7xl)
```

### Grid Layouts

```jsx
// Dashboard Grid (Stats)
<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
  {/* Stat cards */}
</div>

// Content Grid (Cards)
<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
  {/* Content cards */}
</div>

// Two-column Layout
<div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
  <div className="lg:col-span-2">{/* Main content */}</div>
  <div>{/* Sidebar content */}</div>
</div>
```

### Flexbox Patterns

```jsx
// Horizontal with space between
<div className="flex items-center justify-between">...</div>

// Horizontal with gap
<div className="flex items-center gap-4">...</div>

// Vertical stack
<div className="flex flex-col space-y-4">...</div>

// Centered content
<div className="flex items-center justify-center min-h-screen">...</div>
```

---

## Component Patterns

### Navigation Button (Sidebar)

```jsx
<button
  onClick={() => handleNavigation(view)}
  className={`
    w-full text-left flex items-center px-4 py-3 
    rounded-md text-sm font-medium 
    transition-all duration-200
    ${isActive
      ? 'bg-brand-orange text-white shadow-md'
      : 'text-gray-100 hover:bg-white/10 hover:text-white'
    }
  `}
>
  <Icon className="w-5 h-5 mr-3" />
  {label}
</button>
```

### Card Component

```jsx
<div className="bg-white rounded-lg shadow p-6">
  <div className="flex items-center justify-between mb-4">
    <h3 className="text-lg font-semibold text-gray-900">{title}</h3>
    <button className="text-gray-400 hover:text-gray-600">
      <MoreIcon className="w-5 h-5" />
    </button>
  </div>
  <p className="text-sm text-gray-600">{description}</p>
</div>
```

### Primary Button

```jsx
<button className="
  bg-brand-green hover:bg-brand-green-dark 
  text-white font-medium 
  px-4 py-2 rounded-md
  transition-colors duration-200
  focus:outline-none focus:ring-2 focus:ring-brand-green focus:ring-offset-2
">
  Primary Action
</button>
```

### Secondary Button

```jsx
<button className="
  bg-gray-100 hover:bg-gray-200 
  text-gray-700 font-medium 
  px-4 py-2 rounded-md
  transition-colors duration-200
  focus:outline-none focus:ring-2 focus:ring-gray-400 focus:ring-offset-2
">
  Secondary Action
</button>
```

### Ghost Button

```jsx
<button className="
  text-gray-600 hover:text-gray-900 hover:bg-gray-100
  font-medium px-4 py-2 rounded-md
  transition-colors duration-200
  focus:outline-none focus:ring-2 focus:ring-gray-400 focus:ring-offset-2
">
  Ghost Button
</button>
```

### Danger Button

```jsx
<button className="
  bg-red-500 hover:bg-red-600 
  text-white font-medium 
  px-4 py-2 rounded-md
  transition-colors duration-200
  focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2
">
  Delete
</button>
```

### Input Field

```jsx
<div className="space-y-1">
  <label className="text-sm font-medium text-gray-700">
    Label
  </label>
  <input
    type="text"
    className="
      w-full px-3 py-2 
      border border-gray-300 rounded-md
      text-sm text-gray-900
      placeholder:text-gray-400
      focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green
      transition-colors duration-200
    "
    placeholder="Enter text..."
  />
  <p className="text-xs text-gray-500">Helper text goes here</p>
</div>
```

### Select/Dropdown

```jsx
<select className="
  w-full px-3 py-2 
  border border-gray-300 rounded-md
  text-sm text-gray-900 bg-white
  focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green
  transition-colors duration-200
">
  <option>Option 1</option>
  <option>Option 2</option>
</select>
```

### Badge/Tag

```jsx
// Status Badge
<span className="
  inline-flex items-center 
  px-2.5 py-0.5 rounded-full 
  text-xs font-medium
  bg-green-100 text-green-800
">
  Active
</span>

// Category Tag
<span className="
  inline-flex items-center 
  px-2 py-1 rounded-md 
  text-xs font-medium
  bg-gray-100 text-gray-600
">
  Category
</span>
```

### Modal/Dialog

```jsx
// Backdrop
<div className="fixed inset-0 bg-black/50 z-40" />

// Modal Container
<div className="
  fixed inset-0 z-50 
  flex items-center justify-center p-4
">
  <div className="
    bg-white rounded-xl shadow-2xl 
    w-full max-w-lg 
    max-h-[90vh] overflow-hidden
  ">
    {/* Modal Header */}
    <div className="px-6 py-4 border-b border-gray-200">
      <h2 className="text-xl font-semibold text-gray-900">{title}</h2>
    </div>
    
    {/* Modal Body */}
    <div className="px-6 py-4 overflow-y-auto">
      {/* Content */}
    </div>
    
    {/* Modal Footer */}
    <div className="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
      <button className="...">Cancel</button>
      <button className="...">Confirm</button>
    </div>
  </div>
</div>
```

### Table

```jsx
<div className="overflow-x-auto">
  <table className="min-w-full divide-y divide-gray-200">
    <thead className="bg-gray-50">
      <tr>
        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
          Column
        </th>
      </tr>
    </thead>
    <tbody className="bg-white divide-y divide-gray-200">
      <tr className="hover:bg-gray-50 transition-colors">
        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
          Cell
        </td>
      </tr>
    </tbody>
  </table>
</div>
```

### Empty State

```jsx
<div className="text-center py-12">
  <Icon className="mx-auto h-12 w-12 text-gray-400" />
  <h3 className="mt-4 text-lg font-medium text-gray-900">No items found</h3>
  <p className="mt-2 text-sm text-gray-500">
    Get started by creating a new item.
  </p>
  <button className="mt-4 bg-brand-green text-white ...">
    Create New
  </button>
</div>
```

---

## Interactive States

### Hover States

```css
/* Button hover */
hover:bg-brand-green-dark    /* Darker variant */
hover:bg-gray-100            /* Light background */
hover:bg-white/10            /* On dark backgrounds */
hover:text-gray-900          /* Darker text */
hover:shadow-md              /* Elevated shadow */

/* Transition */
transition-all duration-200
transition-colors duration-200
transition-shadow duration-200
```

### Active States

```css
/* Navigation active */
bg-brand-orange text-white shadow-md

/* Button pressed */
active:scale-95              /* Slight shrink effect */
active:bg-brand-green-dark   /* Darker background */
```

### Focus States

```css
/* Standard focus ring */
focus:outline-none focus:ring-2 focus:ring-brand-green focus:ring-offset-2

/* Subtle focus (inputs) */
focus:ring-2 focus:ring-brand-green focus:border-brand-green

/* High contrast focus */
focus:ring-4 focus:ring-brand-green/50
```

### Disabled States

```css
disabled:opacity-50 disabled:cursor-not-allowed

/* Button disabled */
className={`
  ${disabled 
    ? 'opacity-50 cursor-not-allowed bg-gray-300' 
    : 'bg-brand-green hover:bg-brand-green-dark cursor-pointer'
  }
`}
```

### Loading States

```jsx
// Button with spinner
<button disabled className="flex items-center gap-2 opacity-75">
  <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24">...</svg>
  Loading...
</button>

// Skeleton loading
<div className="animate-pulse">
  <div className="h-4 bg-gray-200 rounded w-3/4 mb-2" />
  <div className="h-4 bg-gray-200 rounded w-1/2" />
</div>
```

---

## Icons & Imagery

### Icon Library

Uses **Lucide React** icons (recommended):

```jsx
import { 
  Home, BookOpen, Users, MessageSquare, 
  Settings, Bell, Search, Plus, X, 
  ChevronDown, ChevronRight, Check,
  AlertCircle, Info, CheckCircle, XCircle
} from 'lucide-react';
```

### Icon Sizes

| Size | Class | Pixels | Usage |
|------|-------|--------|-------|
| XS | `w-3 h-3` | 12px | Inline with small text |
| SM | `w-4 h-4` | 16px | Buttons, inputs |
| MD | `w-5 h-5` | 20px | **Navigation, default** |
| LG | `w-6 h-6` | 24px | Headers, emphasis |
| XL | `w-8 h-8` | 32px | Empty states |
| 2XL | `w-12 h-12` | 48px | Hero icons |

### Icon Color Guidelines

```jsx
// Default (inherit text color)
<Icon className="w-5 h-5" />

// Muted
<Icon className="w-5 h-5 text-gray-400" />

// On dark background
<Icon className="w-5 h-5 text-white" />

// Colored
<Icon className="w-5 h-5 text-brand-green" />

// Status colors
<CheckCircle className="w-5 h-5 text-green-500" />
<AlertCircle className="w-5 h-5 text-yellow-500" />
<XCircle className="w-5 h-5 text-red-500" />
```

### Avatar/Image Styling

```jsx
// Avatar
<img 
  className="w-10 h-10 rounded-full object-cover" 
  src={avatarUrl} 
  alt={name} 
/>

// Thumbnail
<img 
  className="w-full h-40 rounded-lg object-cover" 
  src={thumbnailUrl} 
  alt={title} 
/>

// Placeholder
<div className="w-10 h-10 rounded-full bg-brand-green flex items-center justify-center">
  <span className="text-white font-medium text-sm">JD</span>
</div>
```

---

## Animation & Transitions

### Transition Durations

| Duration | Class | Usage |
|----------|-------|-------|
| Fast | `duration-100` | Micro-interactions |
| Default | `duration-200` | **Standard transitions** |
| Medium | `duration-300` | Modals, larger elements |
| Slow | `duration-500` | Page transitions |

### Transition Properties

```css
transition-all      /* All properties (use sparingly) */
transition-colors   /* Color changes only */
transition-shadow   /* Shadow changes */
transition-opacity  /* Fade effects */
transition-transform /* Scale, translate, rotate */
```

### Common Animation Patterns

```jsx
// Hover transition
<button className="transition-colors duration-200 hover:bg-gray-100">
  Click me
</button>

// Shadow elevation on hover
<div className="transition-shadow duration-200 hover:shadow-md">
  Card
</div>

// Scale on hover
<div className="transition-transform duration-200 hover:scale-105">
  Card
</div>

// Fade in
<div className="animate-fade-in">Content</div>

// Spin (loading)
<svg className="animate-spin h-5 w-5">...</svg>

// Pulse (loading skeleton)
<div className="animate-pulse bg-gray-200 h-4 rounded" />

// Bounce (attention)
<div className="animate-bounce">!</div>
```

### Custom Animations (add to CSS)

```css
@keyframes fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slide-up {
  from { 
    opacity: 0;
    transform: translateY(10px);
  }
  to { 
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes scale-in {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.animate-fade-in {
  animation: fade-in 200ms ease-out;
}

.animate-slide-up {
  animation: slide-up 300ms ease-out;
}

.animate-scale-in {
  animation: scale-in 200ms ease-out;
}
```

---

## Z-Index Scale

| Level | Value | Class | Usage |
|-------|-------|-------|-------|
| Base | 0 | `z-0` | Default content |
| Raised | 10 | `z-10` | Sticky headers within content |
| Sidebar | 20 | `z-20` | **Main sidebar** |
| Dropdown | 30 | `z-30` | Dropdown menus |
| Sticky | 40 | `z-40` | Sticky elements |
| Modal Backdrop | 40 | `z-40` | Modal backdrop |
| Modal | 50 | `z-50` | **Modals, dialogs** |
| Popover | 50 | `z-50` | Tooltips, popovers |
| Toast | 60 | `z-60` | Toast notifications |
| Maximum | 9999 | `z-[9999]` | Critical overlays |

---

## Responsive Design

### Breakpoints

| Breakpoint | Min Width | Tailwind Prefix |
|------------|-----------|-----------------|
| Mobile | 0px | (default) |
| SM | 640px | `sm:` |
| MD | 768px | `md:` |
| LG | 1024px | `lg:` |
| XL | 1280px | `xl:` |
| 2XL | 1536px | `2xl:` |

### Responsive Patterns

```jsx
// Stack on mobile, row on desktop
<div className="flex flex-col md:flex-row gap-4">...</div>

// Hide sidebar on mobile
<aside className="hidden lg:block w-64">...</aside>

// Full width on mobile, columns on desktop
<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
  ...
</div>

// Responsive text sizes
<h1 className="text-xl md:text-2xl lg:text-3xl">Title</h1>

// Responsive padding
<div className="p-4 md:p-6 lg:p-8">...</div>

// Mobile menu toggle
<button className="lg:hidden">Menu</button>
```

### Mobile-First Guidelines

1. Design for mobile first, then enhance for larger screens
2. Use `min-width` breakpoints (Tailwind default)
3. Consider touch targets (min 44x44px on mobile)
4. Collapse sidebar to hamburger menu on mobile
5. Stack cards vertically on narrow screens

---

## Dark Mode Considerations

While the current implementation focuses on light mode, here's the dark mode palette for future reference:

### Dark Mode Colors

```css
/* Dark mode backgrounds */
--color-bg-dark-primary: #111827;    /* gray-900 */
--color-bg-dark-secondary: #1F2937;  /* gray-800 */
--color-bg-dark-tertiary: #374151;   /* gray-700 */

/* Dark mode text */
--color-text-dark-primary: #F9FAFB;  /* gray-50 */
--color-text-dark-secondary: #D1D5DB; /* gray-300 */
--color-text-dark-muted: #9CA3AF;    /* gray-400 */

/* Dark mode borders */
--color-border-dark: #374151;        /* gray-700 */
```

### Dark Mode Implementation

```jsx
// Using Tailwind dark mode
<div className="bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100">
  ...
</div>

// Card in dark mode
<div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700">
  ...
</div>
```

---

## Accessibility

### Color Contrast

- Maintain minimum **4.5:1** contrast ratio for normal text
- Maintain minimum **3:1** contrast ratio for large text (18px+)
- The brand-green (#2D5A3D) on white provides sufficient contrast
- White text on brand-green provides sufficient contrast

### Focus Management

```jsx
// Always provide visible focus states
className="focus:outline-none focus:ring-2 focus:ring-brand-green focus:ring-offset-2"

// Skip to main content link
<a href="#main" className="sr-only focus:not-sr-only focus:absolute ...">
  Skip to main content
</a>
```

### Screen Reader

```jsx
// Screen reader only text
<span className="sr-only">Open menu</span>

// Aria labels
<button aria-label="Close modal">
  <XIcon className="w-5 h-5" />
</button>

// Aria-hidden for decorative elements
<Icon aria-hidden="true" className="w-5 h-5" />
```

### Interactive Elements

- Minimum touch target: **44x44px**
- All interactive elements must be keyboard accessible
- Use semantic HTML (`<button>`, `<a>`, `<input>`)
- Provide loading and disabled states

---

## Code Examples

### Complete Card Component

```tsx
import React from 'react';

interface CardProps {
  title: string;
  description?: string;
  children?: React.ReactNode;
  className?: string;
}

export const Card: React.FC<CardProps> = ({ 
  title, 
  description, 
  children,
  className = '' 
}) => {
  return (
    <div className={`
      bg-white rounded-lg shadow p-6
      transition-shadow duration-200 hover:shadow-md
      ${className}
    `}>
      <h3 className="text-lg font-semibold text-gray-900 mb-2">
        {title}
      </h3>
      {description && (
        <p className="text-sm text-gray-600 mb-4">{description}</p>
      )}
      {children}
    </div>
  );
};
```

### Complete Button Component

```tsx
import React from 'react';

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';
type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  isLoading?: boolean;
  children: React.ReactNode;
}

const variantStyles: Record<ButtonVariant, string> = {
  primary: 'bg-brand-green hover:bg-brand-green-dark text-white',
  secondary: 'bg-gray-100 hover:bg-gray-200 text-gray-700',
  ghost: 'hover:bg-gray-100 text-gray-600 hover:text-gray-900',
  danger: 'bg-red-500 hover:bg-red-600 text-white',
};

const sizeStyles: Record<ButtonSize, string> = {
  sm: 'px-3 py-1.5 text-xs',
  md: 'px-4 py-2 text-sm',
  lg: 'px-6 py-3 text-base',
};

export const Button: React.FC<ButtonProps> = ({
  variant = 'primary',
  size = 'md',
  isLoading = false,
  disabled,
  children,
  className = '',
  ...props
}) => {
  return (
    <button
      disabled={disabled || isLoading}
      className={`
        inline-flex items-center justify-center
        font-medium rounded-md
        transition-colors duration-200
        focus:outline-none focus:ring-2 focus:ring-offset-2
        ${variantStyles[variant]}
        ${sizeStyles[size]}
        ${(disabled || isLoading) ? 'opacity-50 cursor-not-allowed' : ''}
        ${variant === 'primary' ? 'focus:ring-brand-green' : 'focus:ring-gray-400'}
        ${className}
      `}
      {...props}
    >
      {isLoading && (
        <svg 
          className="animate-spin -ml-1 mr-2 h-4 w-4" 
          fill="none" 
          viewBox="0 0 24 24"
        >
          <circle 
            className="opacity-25" 
            cx="12" cy="12" r="10" 
            stroke="currentColor" 
            strokeWidth="4"
          />
          <path 
            className="opacity-75" 
            fill="currentColor" 
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
          />
        </svg>
      )}
      {children}
    </button>
  );
};
```

### Theme Context Example

```tsx
import React, { createContext, useContext, useState } from 'react';

interface ThemeColors {
  primary: string;
  primaryDark: string;
  accent: string;
  background: string;
  surface: string;
  text: string;
  textSecondary: string;
  border: string;
}

interface Theme {
  name: string;
  colors: ThemeColors;
}

const lightTheme: Theme = {
  name: 'light',
  colors: {
    primary: '#2D5A3D',
    primaryDark: '#1E3D2A',
    accent: '#E07A3D',
    background: '#F9FAFB',
    surface: '#FFFFFF',
    text: '#111827',
    textSecondary: '#6B7280',
    border: '#E5E7EB',
  },
};

interface ThemeContextType {
  theme: Theme;
  setTheme: (theme: Theme) => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ 
  children 
}) => {
  const [theme, setTheme] = useState<Theme>(lightTheme);

  return (
    <ThemeContext.Provider value={{ theme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within ThemeProvider');
  }
  return context;
};
```

---

## Tailwind Configuration Reference

Add these custom colors and fonts to your `tailwind.config.js`:

```js
module.exports = {
  content: ['./src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        'brand-green': {
          DEFAULT: '#2D5A3D',
          light: '#3D7A52',
          dark: '#1E3D2A',
        },
        'brand-orange': {
          DEFAULT: '#E07A3D',
          light: '#F09A5D',
          dark: '#C56A30',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
        serif: ['Fraunces', 'Georgia', 'Times New Roman', 'serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'Consolas', 'monospace'],
      },
      animation: {
        'fade-in': 'fade-in 200ms ease-out',
        'slide-up': 'slide-up 300ms ease-out',
        'scale-in': 'scale-in 200ms ease-out',
      },
      keyframes: {
        'fade-in': {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        'slide-up': {
          '0%': { opacity: '0', transform: 'translateY(10px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        'scale-in': {
          '0%': { opacity: '0', transform: 'scale(0.95)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
      },
    },
  },
  plugins: [],
};
```

---

## Quick Reference Cheat Sheet

### Most Used Classes

```
// Layouts
flex items-center justify-between
grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6
space-y-4
w-64 (sidebar)
h-16 (header)

// Spacing
p-4, p-6 (padding)
px-4 py-2, px-4 py-3 (buttons)
gap-4, gap-6 (grids)
space-y-2 (nav items)

// Typography
font-serif text-xl font-extrabold tracking-tight (brand name)
font-sans text-sm font-medium (nav, buttons)
font-sans text-lg font-semibold (card titles)
font-sans text-2xl font-bold (page titles)
text-xs text-gray-400 (captions)
font-mono (code)

// Font Families
font-sans  → Inter (default, UI)
font-serif → Fraunces (brand, elegant headings)
font-mono  → JetBrains Mono (code)

// Colors
bg-brand-green, bg-brand-orange
bg-white, bg-gray-50, bg-gray-100
text-white, text-gray-900, text-gray-600, text-gray-400

// Borders & Radius
rounded-md (buttons, inputs)
rounded-lg (cards)
rounded-xl (modals)
rounded-full (badges, avatars)
border border-gray-200

// Shadows
shadow (cards)
shadow-md (active nav)
shadow-xl (sidebar)
shadow-2xl (modals)

// Transitions
transition-all duration-200
transition-colors duration-200
hover:bg-white/10 (on dark)
hover:bg-gray-100 (on light)

// Focus
focus:outline-none focus:ring-2 focus:ring-brand-green focus:ring-offset-2

// Header
h-16 bg-white border-b border-gray-200 px-6
flex items-center justify-between
sticky top-0 z-10
```

### Font Quick Reference

```
// Brand Name (Fraunces - Serif)
<h1
  className="font-serif text-xl font-extrabold text-gray-900 tracking-tight"
  style={{ fontVariationSettings: "'opsz' 24, 'SOFT' 0, 'WONK' 0" }}
>
  Brandname
</h1>

// Brand Name with preferred Fraunces settings (explicit)
<h1
  style={{ fontVariationSettings: "'opsz' 24, 'SOFT' 0, 'WONK' 0" }}
  className="font-serif text-3xl font-extrabold text-gray-900"
>
  Brandname
</h1>

// Page Title (Sans)
<h1 className="font-sans text-2xl font-bold text-gray-900">
  Dashboard
</h1>

// Section Header (Sans)
<h2 className="font-sans text-lg font-semibold text-gray-900">
  Recent Activity
</h2>

// Body Text (Sans - default)
<p className="text-sm text-gray-600">
  Description text
</p>

// Code (Mono)
<code className="font-mono text-sm bg-gray-100 px-1 rounded">
  npm install
</code>

// Fraunces Variable Axes (for advanced styling)
font-weight: 800;                        // Preferred weight
font-variation-settings: 'opsz' 24;      // Preferred optical size (9-144)
font-variation-settings: 'WONK' 0;       // Preferred wonk (0-1)
font-variation-settings: 'SOFT' 0;       // Preferred softness (0-100)
```

---

*This style guide is version X.Y.Z • Academic Modern theme. Last updated January 2026.*
