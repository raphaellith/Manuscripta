# Manuscripta Design System & Style Guide

> A comprehensive style guide for React/Tailwind projects based on the Manuscripta Teacher Portal prototype.

---

## Table of Contents

1. [Color System](#color-system)
2. [Typography](#typography)
3. [Spacing & Layout](#spacing--layout)
4. [Border Radius](#border-radius)
5. [Shadows](#shadows)
6. [Components](#components)
7. [Animations](#animations)
8. [Icons](#icons)
9. [Responsive Design](#responsive-design)
10. [Theming System](#theming-system)
11. [Tailwind Configuration](#tailwind-configuration)

---

## Color System

### Brand Colors (Default Theme)

The color system uses CSS custom properties for themability. All colors are defined in `:root` and can be swapped dynamically.

#### Primary Brand Colors

| Token | CSS Variable | Hex Value | Tailwind Class | Usage |
|-------|-------------|-----------|----------------|-------|
| Brand Cream | `--color-brand-cream` | `#F8F7F5` | `bg-brand-cream` | Page background, light surfaces |
| Brand Orange | `--color-brand-orange` | `#FF6106` | `bg-brand-orange`, `text-brand-orange` | Primary action, emphasis, CTAs |
| Brand Orange Light | `--color-brand-orange-light` | `#FEE3DA` | `bg-brand-orange-light` | Light accent, selection highlight |
| Brand Orange Dark | `--color-brand-orange-dark` | `#E55605` | `bg-brand-orange-dark` | Hover state for orange buttons |
| Brand Yellow | `--color-brand-yellow` | `#FFE782` | `bg-brand-yellow` | Pastel accent, worksheets, highlights |
| Brand Blue | `--color-brand-blue` | `#ABD8F9` | `bg-brand-blue` | Pastel accent, lessons, info states |
| Brand Green | `--color-brand-green` | `#15502E` | `bg-brand-green` | Secondary action, success states |
| Brand Gray | `--color-brand-gray` | `#F3F3F3` | `bg-brand-gray` | Neutral backgrounds, input fills |

#### Text Colors

| Token | CSS Variable | Hex Value | Tailwind Class | Usage |
|-------|-------------|-----------|----------------|-------|
| Text Heading | `--color-text-heading` | `#212631` | `text-text-heading` | Headlines, titles, emphasis |
| Text Body | `--color-text-body` | `#14201E` | `text-text-body` | Body copy, paragraphs |
| Text on Yellow | `--color-text-on-yellow` | `#5C4A00` | `text-text-onYellow` | Text on yellow backgrounds |
| Text on Blue | `--color-text-on-blue` | `#1A3A5C` | `text-text-onBlue` | Text on blue backgrounds |

#### Semantic Status Colors

| State | Color | Tailwind Classes | Usage |
|-------|-------|------------------|-------|
| Success | Brand Green | `text-brand-green`, `bg-brand-green` | Correct answers, deployed status |
| Warning | Brand Orange | `text-brand-orange`, `bg-brand-orange` | Help needed, draft status |
| Error | Red 500 | `text-red-500`, `bg-red-500` | Incorrect, disconnected |
| Info | Brand Blue | `text-brand-blue`, `bg-brand-blue` | On task, informational |
| Neutral | Gray tones | `text-gray-500`, `bg-gray-100` | Disabled, idle states |

---

## Typography

### Font Families

| Type | Font | Tailwind Class | Usage |
|------|------|----------------|-------|
| Serif (Display) | `'Fraunces', serif` | `font-serif` | Main headers **only**, logo, significant headings |
| Sans-serif (Body) | `'IBM Plex Sans', sans-serif` | `font-sans` | Body text, UI elements, labels, subheadings |

> [!IMPORTANT]
> The serif font (Fraunces) should only be used for **main headers** or **highly significant headings**. Overuse diminishes its impact. Use sans-serif for all other text including subheadings, labels, and body copy.

### Google Fonts Import

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Fraunces:opsz,wght@9..144,300;400;500;600&family=IBM+Plex+Sans:wght@300;400;500;600&display=swap" rel="stylesheet">
```

### Typography Scale

#### Page Titles (Main Headers Only)
```css
font-family: 'Fraunces', serif;
font-size: 2rem;        /* text-2xl = 24px, or text-3xl = 30px */
font-weight: 500;       /* font-medium */
color: var(--color-text-heading);
```
Tailwind: `text-2xl font-serif font-medium text-text-heading`

#### Section Headers
```css
font-family: 'IBM Plex Sans', sans-serif;
font-size: 1.25rem;     /* text-xl = 20px */
font-weight: 600;       /* font-semibold */
color: var(--color-text-heading);
```
Tailwind: `text-xl font-sans font-semibold text-text-heading`

#### Card/Panel Titles
```css
font-family: 'IBM Plex Sans', sans-serif;
font-size: 1.125rem;    /* text-lg = 18px */
font-weight: 600;       /* font-semibold */
color: var(--color-text-heading);
```
Tailwind: `text-lg font-sans font-semibold text-text-heading`

#### Body Text
```css
font-family: 'IBM Plex Sans', sans-serif;
font-size: 0.875rem;    /* text-sm = 14px */
font-weight: 400;       /* font-normal */
color: var(--color-text-body);
line-height: 1.7;       /* leading-relaxed */
```
Tailwind: `text-sm font-sans text-text-body leading-relaxed`

#### Labels & Captions
```css
font-family: 'IBM Plex Sans', sans-serif;
font-size: 0.875rem;    /* text-sm = 14px */
font-weight: 500;       /* font-medium */
color: var(--color-text-heading);
```
Tailwind: `text-sm font-sans font-medium text-text-heading`

#### Helper Text / Metadata
```css
font-family: 'IBM Plex Sans', sans-serif;
font-size: 0.75rem;     /* text-xs = 12px */
font-weight: 400;       /* font-normal */
color: #6B7280;         /* gray-500 */
```
Tailwind: `text-xs font-sans text-gray-500`

#### Status Badges
```css
font-family: 'IBM Plex Sans', sans-serif;
font-size: 0.75rem;     /* text-xs = 12px */
font-weight: 600;       /* font-semibold */
text-transform: uppercase;
letter-spacing: 0.05em; /* tracking-wide */
```
Tailwind: `text-xs font-sans font-semibold uppercase tracking-wide`

---

## Spacing & Layout

### Core Spacing Scale

Uses Tailwind's default spacing scale (1 unit = 4px):

| Size | Pixels | Tailwind | Usage |
|------|--------|----------|-------|
| 1 | 4px | `p-1`, `m-1`, `gap-1` | Tiny internal spacing |
| 2 | 8px | `p-2`, `m-2`, `gap-2` | Small compact elements |
| 3 | 12px | `p-3`, `m-3`, `gap-3` | Input padding, tight spacing |
| 4 | 16px | `p-4`, `m-4`, `gap-4` | Standard component spacing |
| 5 | 20px | `p-5`, `m-5`, `gap-5` | Mid-range spacing |
| 6 | 24px | `p-6`, `m-6`, `gap-6` | Card padding, section gaps |
| 8 | 32px | `p-8`, `m-8`, `gap-8` | Large padding, main content |
| 10 | 40px | `p-10`, `m-10` | Extra large / hero areas |

### Layout Patterns

#### Page Container
```html
<main className="max-w-7xl mx-auto p-8 w-full">
```

#### Grid Layouts
- **4-column stats:** `grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4`
- **2-column forms:** `grid grid-cols-1 md:grid-cols-2 gap-8`
- **Card grids:** `grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6`
- **Device grid:** `grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-6`

#### Flex Layouts
- **Horizontal stack:** `flex items-center gap-4`
- **Between alignment:** `flex justify-between items-center`
- **Vertical stack:** `flex flex-col gap-4` or `space-y-4`

---

## Border Radius

| Size | Pixels | Tailwind | Usage |
|------|--------|----------|-------|
| Default | 6px | `rounded` or `rounded-md` | Buttons, inputs, small cards |
| Large | 8px | `rounded-lg` | Cards, modals, panels |
| Extra Large | 12px | `rounded-xl` | Feature cards, navigation cards |
| 2XL | 16px | `rounded-2xl` | Header container, large panels |
| Full | 9999px | `rounded-full` | Avatars, badges, pills |

---

## Shadows

### Shadow Tokens

| Token | CSS Value | Tailwind | Usage |
|-------|-----------|----------|-------|
| Soft | `0 4px 20px rgba(0,0,0,0.08)` | `shadow-soft` | Cards, elevated panels |
| Standard | Default | `shadow-sm` | Buttons, small elements |
| Medium | Default | `shadow-md` | Hover states |
| Large | Default | `shadow-lg` | Dropdown menus |
| 2XL | Default | `shadow-2xl` | Modals, overlays |

### Custom Shadow Definition
```javascript
boxShadow: {
  soft: '0 4px 20px rgba(0,0,0,0.08)',
}
```

---

## Components

### Buttons

#### Primary Button (Orange)
```html
<button className="px-6 py-3 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm">
  Save Changes
</button>
```
**Sizes:**
- Small: `px-3 py-1.5 text-sm`
- Default: `px-6 py-3`
- Large: `px-8 py-4`

**States:**
- Hover: `hover:bg-brand-orange-dark`
- Disabled: `disabled:bg-gray-300 disabled:cursor-not-allowed`

#### Secondary Button (Green)
```html
<button className="px-6 py-3 bg-brand-green text-white font-sans font-medium rounded-md hover:bg-green-800 transition-colors shadow-sm">
  Export
</button>
```

#### Outlined Button
```html
<button className="px-6 py-3 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors">
  Cancel
</button>
```

#### Light/Ghost Button
```html
<button className="px-4 py-2 bg-brand-orange-light text-brand-orange hover:bg-orange-200 font-sans font-medium rounded-md transition-colors border border-transparent">
  Lock Screens
</button>
```

#### Text Button/Link
```html
<button className="text-sm font-medium text-brand-orange hover:text-brand-orange-dark transition-colors">
  + Add Content
</button>
```

---

### Cards

#### Standard Card
```html
<div className="bg-white rounded-lg p-8 shadow-soft border border-gray-100 transition-shadow hover:shadow-md">
  <!-- Content -->
</div>
```

#### Card with Top Border Accent
```html
<div className="bg-white rounded-lg p-8 shadow-soft border border-gray-100 border-t-4 border-t-brand-orange">
  <!-- Content -->
</div>
```
Accent colors: `border-t-brand-orange`, `border-t-brand-blue`, `border-t-brand-green`

#### Interactive/Clickable Card
```html
<div className="group bg-white rounded-xl border-2 border-gray-100 p-6 cursor-pointer hover:border-brand-orange hover:shadow-lg transition-all">
  <h3 className="text-lg font-semibold text-text-heading group-hover:text-brand-orange transition-colors">
    Card Title
  </h3>
</div>
```

---

### Form Inputs

#### Text Input
```html
<input 
  type="text"
  className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none transition-all"
  placeholder="Enter text..."
/>
```

#### Select Dropdown
```html
<select className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed">
  <option value="">-- Select Option --</option>
</select>
```

#### Textarea
```html
<textarea 
  className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none transition-all resize-none"
  rows={4}
></textarea>
```

#### Input Label
```html
<label className="font-sans font-medium text-text-heading text-sm mb-2 block">
  Field Label
</label>
```

---

### Toggle Switch

```html
<button
  className={`w-12 h-6 rounded-full p-0.5 transition-colors duration-300 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-brand-orange ${enabled ? 'bg-brand-orange' : 'bg-gray-300'}`}
>
  <div className={`w-5 h-5 rounded-full bg-white shadow-sm transform transition-transform duration-300 ${enabled ? 'translate-x-6' : 'translate-x-0'}`} />
</button>
```

---

### Modals

#### Modal Backdrop
```html
<div className="fixed inset-0 bg-text-heading/20 backdrop-blur-sm flex items-center justify-center z-50 p-4">
```

#### Modal Container
```html
<div className="bg-white rounded-xl shadow-2xl w-full max-w-4xl max-h-[85vh] flex flex-col animate-fade-in-up overflow-hidden">
  <!-- Header -->
  <div className="flex justify-between items-center p-6 border-b border-gray-100">
    <h2 className="text-2xl font-serif text-text-heading">Modal Title</h2>
    <button className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
      <!-- Close icon -->
    </button>
  </div>
  
  <!-- Body -->
  <div className="flex-1 overflow-y-auto p-6">
    <!-- Content -->
  </div>
  
  <!-- Footer -->
  <div className="p-4 border-t border-gray-100 bg-gray-50 flex justify-end gap-3">
    <!-- Actions -->
  </div>
</div>
```

---

### Status Badges

#### Content Type Badges
```html
<!-- Lesson (Blue) -->
<span className="text-xs font-sans font-semibold px-2 py-1 rounded-md uppercase tracking-wide bg-brand-blue/20 text-blue-900">
  Lesson
</span>

<!-- Worksheet (Yellow) -->
<span className="text-xs font-sans font-semibold px-2 py-1 rounded-md uppercase tracking-wide bg-brand-yellow/30 text-yellow-900">
  Worksheet
</span>

<!-- Quiz (Orange) -->
<span className="text-xs font-sans font-semibold px-2 py-1 rounded-md uppercase tracking-wide bg-brand-orange-light text-brand-orange-dark">
  Quiz
</span>

<!-- Reading (Green) -->
<span className="text-xs font-sans font-semibold px-2 py-1 rounded-md uppercase tracking-wide bg-brand-green/20 text-green-900">
  Reading
</span>

<!-- PDF (Red) -->
<span className="text-xs font-sans font-semibold px-2 py-1 rounded-md uppercase tracking-wide bg-red-50 text-red-600">
  PDF
</span>
```

#### Status Badges (Deployed/Draft)
```html
<!-- Deployed -->
<span className="font-medium text-brand-green">Deployed</span>

<!-- Draft -->
<span className="font-medium text-brand-orange">Draft</span>
```

#### Device Status Badges
```html
<!-- On Task -->
<span className="text-xs font-semibold px-2 py-1 rounded-full uppercase tracking-wide bg-brand-blue text-blue-900">
  On Task
</span>

<!-- Idle -->
<span className="text-xs font-semibold px-2 py-1 rounded-full uppercase tracking-wide bg-brand-green text-white">
  Idle
</span>

<!-- Needs Help -->
<span className="text-xs font-semibold px-2 py-1 rounded-full uppercase tracking-wide bg-brand-orange text-white">
  Needs Help
</span>

<!-- Locked -->
<span className="text-xs font-semibold px-2 py-1 rounded-full uppercase tracking-wide bg-yellow-600 text-white">
  Locked
</span>

<!-- Disconnected -->
<span className="text-xs font-semibold px-2 py-1 rounded-full uppercase tracking-wide bg-gray-600 text-white">
  Disconnected
</span>
```

---

### Navigation

#### Header Container
```html
<header className="bg-white/95 backdrop-blur-xl border border-white/20 px-8 h-20 flex justify-between items-center shadow-soft w-full max-w-7xl rounded-2xl ring-1 ring-gray-900/5 transition-all">
```

#### Nav Items (Pill Style)
```html
<!-- Active -->
<button className="px-4 py-2 text-sm font-medium rounded-lg bg-brand-green/10 text-brand-green">
  Library
</button>

<!-- Inactive -->
<button className="px-4 py-2 text-sm font-medium rounded-lg text-text-body/60 hover:bg-gray-50 hover:text-brand-green transition-all duration-200">
  Dashboard
</button>
```

#### Breadcrumbs
```html
<div className="flex items-center gap-2 text-sm mb-6 flex-wrap">
  <button className="font-medium text-brand-orange hover:text-brand-orange-dark transition-colors">
    All Collections
  </button>
  <span className="text-gray-400">›</span>
  <span className="font-medium text-text-heading">Current Page</span>
</div>
```

---

### Avatar

```html
<div className="w-10 h-10 bg-gradient-to-br from-brand-green to-emerald-800 text-white rounded-full flex items-center justify-center font-sans font-bold shadow-sm ring-2 ring-white">
  JK
</div>
```

---

### Alert/Banner

```html
<div className="mb-6 bg-brand-orange-light border border-brand-orange rounded-lg p-4 flex items-center gap-4">
  <div className="bg-brand-orange text-white p-2 rounded-full">
    <!-- Icon -->
  </div>
  <div className="flex-1">
    <p className="font-sans font-semibold text-brand-orange-dark">Alert Title</p>
    <p className="font-sans text-sm text-brand-orange">Description text</p>
  </div>
  <button className="px-4 py-2 bg-brand-orange text-white text-sm font-medium rounded-md hover:bg-brand-orange-dark transition-colors">
    Action
  </button>
</div>
```

---

### Toast Notification

```html
<div className="fixed top-40 right-4 z-50 animate-slide-in">
  <div className="bg-white rounded-lg shadow-2xl border-l-4 border-brand-orange p-4 flex items-start gap-4 max-w-sm">
    <div className="bg-brand-orange-light p-2 rounded-full animate-pulse">
      <!-- Icon -->
    </div>
    <div className="flex-1">
      <p className="font-sans font-semibold text-text-heading">Toast Title</p>
      <p className="font-sans text-sm text-gray-600 mt-1">Description</p>
      <div className="flex gap-2 mt-3">
        <button className="px-3 py-1 bg-brand-orange text-white text-xs font-medium rounded hover:bg-brand-orange-dark transition-colors">
          Acknowledge
        </button>
        <button className="px-3 py-1 bg-gray-100 text-gray-600 text-xs font-medium rounded hover:bg-gray-200 transition-colors">
          Dismiss
        </button>
      </div>
    </div>
  </div>
</div>
```

---

### Tables

```html
<table className="w-full text-sm">
  <thead>
    <tr className="border-b border-gray-200">
      <th className="text-left py-2 px-3 font-sans font-medium text-gray-600">Column</th>
    </tr>
  </thead>
  <tbody>
    <tr className="border-b border-gray-50 hover:bg-gray-50">
      <td className="py-2 px-3 font-sans text-text-body">Cell content</td>
    </tr>
  </tbody>
</table>
```

---

### File Upload Zone

```html
<div className="border-2 border-dashed border-gray-200 rounded-lg p-10 text-center hover:border-brand-orange/50 transition-colors bg-brand-gray/30">
  <div className="flex justify-center mb-4">
    <div className="p-4 bg-brand-orange-light rounded-full">
      <!-- Upload icon -->
    </div>
  </div>
  <p className="font-medium text-text-heading">Drag & drop files here or</p>
  <label className="mt-1 inline-block cursor-pointer text-brand-orange font-semibold hover:text-brand-orange-dark underline decoration-2 underline-offset-2">
    browse from computer
    <input type="file" className="sr-only" multiple />
  </label>
  <p className="text-sm text-gray-500 mt-2">PDF, TXT, DOCX up to 10MB</p>
</div>
```

---

### Chat Messages

```html
<!-- User Message -->
<div className="flex justify-end mb-6">
  <div className="max-w-lg p-5 rounded-lg font-sans leading-relaxed shadow-sm bg-brand-orange text-white rounded-br-none">
    User message content
  </div>
</div>

<!-- AI Message -->
<div className="flex justify-start mb-6">
  <div className="max-w-lg p-5 rounded-lg font-sans leading-relaxed shadow-sm bg-white text-text-body border border-gray-100 rounded-bl-none">
    AI response content
  </div>
</div>
```

---

## Animations

### Keyframe Definitions

```css
/* Fade In Up - Modal entrance */
@keyframes fade-in-up {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
.animate-fade-in-up { animation: fade-in-up 0.3s ease-out forwards; }

/* Slide In - Toast notifications */
@keyframes slide-in {
  from { opacity: 0; transform: translateX(100px); }
  to { opacity: 1; transform: translateX(0); }
}
.animate-slide-in { animation: slide-in 0.3s ease-out forwards; }

/* Slow Pulse - Alert banners */
@keyframes pulse-slow {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}
.animate-pulse-slow { animation: pulse-slow 2s ease-in-out infinite; }

/* Slow Bounce - Hand raised icon */
@keyframes bounce-slow {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}
.animate-bounce-slow { animation: bounce-slow 1s ease-in-out infinite; }

/* Pulse Border - Attention state */
@keyframes pulse-border {
  0%, 100% { box-shadow: 0 0 0 0 rgba(237, 135, 51, 0.4); }
  50% { box-shadow: 0 0 0 8px rgba(237, 135, 51, 0); }
}
.animate-pulse-border { animation: pulse-border 2s ease-in-out infinite; }
```

### Transitions

Standard transition classes used throughout:
- **Color/general:** `transition-colors`
- **All properties:** `transition-all`
- **Duration:** `duration-200` (default), `duration-300`
- **Shadow:** `transition-shadow`

---

## Icons

The design system uses inline SVG icons with these standard properties:

### Icon Sizes

| Size | Class | Usage |
|------|-------|-------|
| Extra Small | `h-3 w-3` | Inline indicators |
| Small | `h-4 w-4` | Buttons, badges |
| Medium | `h-5 w-5` | Navigation, actions |
| Default | `h-6 w-6` | Standard icons |
| Large | `h-8 w-8` | Card icons, features |
| Extra Large | `h-10 w-10` | Upload areas |
| Hero | `h-16 w-16` | Empty states |

### Icon Style
```html
<svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
  <path strokeLinecap="round" strokeLinejoin="round" d="..." />
</svg>
```

For filled icons: `fill="currentColor"` instead of stroke.

---

## Responsive Design

### Breakpoints

Uses Tailwind's default breakpoints:

| Prefix | Min Width | Usage |
|--------|-----------|-------|
| (none) | 0px | Mobile first |
| `sm` | 640px | Large phones |
| `md` | 768px | Tablets |
| `lg` | 1024px | Laptops |
| `xl` | 1280px | Desktops |
| `2xl` | 1536px | Large screens |

### Responsive Patterns

```html
<!-- Hide on mobile, show on larger screens -->
<div className="hidden md:block">...</div>

<!-- Stack on mobile, row on desktop -->
<div className="flex flex-col sm:flex-row gap-4">...</div>

<!-- Responsive grid -->
<div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">...</div>

<!-- Responsive text -->
<p className="text-sm md:text-base lg:text-lg">...</p>
```

---

## Theming System

### Theme Structure

Each theme defines the following CSS variables:

```typescript
interface Theme {
  name: string;
  colors: {
    '--color-brand-cream': string;
    '--color-brand-orange': string;
    '--color-brand-orange-light': string;
    '--color-brand-orange-dark': string;
    '--color-brand-yellow': string;
    '--color-brand-blue': string;
    '--color-brand-green': string;
    '--color-brand-gray': string;
    '--color-text-heading': string;
    '--color-text-body': string;
    '--color-text-on-yellow': string;
    '--color-text-on-blue': string;
  };
}
```

### Available Theme Presets

| Theme Key | Name | Description |
|-----------|------|-------------|
| `default` | Default | Warm cream & orange |
| `boldPrimary` | Bold Primary & Dark | Hot pink with dark accents |
| `earthyGeometric` | Earthy & Geometric | Terracotta and teal |
| `navyGold` | Navy & Gold Contrast | Professional navy with gold |
| `mutedGreens` | Muted Greens & Browns | Organic, natural tones |
| `deepBluePink` | Deep Blue & Soft Pink | Soft romantic palette |
| `pastelPurple` | Pastel Purple & Yellow | Playful pastels |
| `vintageReds` | Vintage Reds & Creams | Classic, warm feeling |
| `darkOxblood` | Dark Grey & Oxblood | Rich, sophisticated |
| `patriotic` | Patriotic Tones | Red, white, and blue |
| `vibrantSunset` | Vibrant Sunset | Warm sunset colors |
| `darkWine` | Dark Wine & Sandy Brown | Elegant wine tones |
| `softPurples` | Soft Purples & Grey | Gentle lavender palette |
| `peachGreens` | Peach & Muted Greens | Fresh and modern |
| `pastelLime` | Pastel Lime & Blue | Light and airy |
| `earthyMint` | Earthy Browns & Mint | Natural with mint accent |
| `vibrantPrimary` | Vibrant Primary Mix | Bold primary colors |
| `blueGreySunset` | Blue Grey & Sunset Tones | Cool with warm accents |

### Theme Application

```typescript
useEffect(() => {
  const theme = themes[currentTheme];
  if (theme) {
    Object.entries(theme.colors).forEach(([key, value]) => {
      document.documentElement.style.setProperty(key, value);
    });
  }
}, [currentTheme]);
```

---

## Tailwind Configuration

### Complete Configuration

```javascript
tailwind.config = {
  theme: {
    extend: {
      colors: {
        brand: {
          cream: 'var(--color-brand-cream)',
          orange: {
            DEFAULT: 'var(--color-brand-orange)',
            light: 'var(--color-brand-orange-light)',
            dark: 'var(--color-brand-orange-dark)',
          },
          yellow: 'var(--color-brand-yellow)',
          blue: 'var(--color-brand-blue)',
          green: 'var(--color-brand-green)',
          gray: 'var(--color-brand-gray)',
        },
        text: {
          heading: 'var(--color-text-heading)',
          body: 'var(--color-text-body)',
          onYellow: 'var(--color-text-on-yellow)',
          onBlue: 'var(--color-text-on-blue)',
        }
      },
      fontFamily: {
        serif: ['Fraunces', 'serif'],
        sans: ['IBM Plex Sans', 'sans-serif'],
      },
      borderRadius: {
        DEFAULT: '6px',
        md: '6px',
        lg: '8px',
      },
      boxShadow: {
        soft: '0 4px 20px rgba(0,0,0,0.08)',
      }
    }
  }
}
```

### CSS Variables (index.css)

```css
:root {
  --color-brand-cream: #F8F7F5;
  --color-brand-orange: #FF6106;
  --color-brand-orange-light: #FEE3DA;
  --color-brand-orange-dark: #E55605;
  --color-brand-yellow: #FFE782;
  --color-brand-blue: #ABD8F9;
  --color-brand-green: #15502E;
  --color-brand-gray: #F3F3F3;
  --color-text-heading: #212631;
  --color-text-body: #14201E;
  --color-text-on-yellow: #5C4A00;
  --color-text-on-blue: #1A3A5C;
}

body {
  background-color: var(--color-brand-cream);
  color: var(--color-text-body);
}
```

---

## Quick Reference

### Most Used Classes

| Pattern | Classes |
|---------|---------|
| Page background | `bg-brand-cream` |
| Card container | `bg-white rounded-lg p-8 shadow-soft border border-gray-100` |
| Primary button | `px-6 py-3 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm` |
| Text input | `w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none` |
| Section title | `text-xl font-sans font-semibold text-text-heading` |
| Body text | `font-sans text-text-body` |
| Helper text | `text-sm text-gray-500` |
| Selection highlight | `selection:bg-brand-orange-light selection:text-brand-orange` |

---

## Design Principles

1. **Warmth & Accessibility**: The cream backgrounds and warm orange accents create a welcoming, non-intimidating environment for educators.

2. **Clear Hierarchy**: Serif fonts for main headings, sans-serif for everything else creates visual distinction without complexity.

3. **Consistent Spacing**: Generous padding (p-6, p-8) creates breathing room while maintaining information density.

4. **Interactive Feedback**: All interactive elements have clear hover states and transitions (usually 200ms).

5. **Status Communication**: Color-coded badges and states (green for success/deployed, orange for attention/draft) provide instant status recognition.

6. **Themeable Foundation**: CSS variables enable complete theme customization while maintaining design consistency.

---

*Last updated: December 2025*
