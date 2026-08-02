---
name: Axiom Workflow System
colors:
  surface: '#f9f9ff'
  surface-dim: '#d0daf0'
  surface-bright: '#f9f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f0f3ff'
  surface-container: '#e7eeff'
  surface-container-high: '#dee8ff'
  surface-container-highest: '#d9e3f9'
  on-surface: '#121c2c'
  on-surface-variant: '#5a403e'
  inverse-surface: '#273141'
  inverse-on-surface: '#ebf1ff'
  outline: '#8e706d'
  outline-variant: '#e2bebb'
  surface-tint: '#b52426'
  primary: '#a2141b'
  on-primary: '#ffffff'
  primary-container: '#c53030'
  on-primary-container: '#ffe4e1'
  inverse-primary: '#ffb3ad'
  secondary: '#545f72'
  on-secondary: '#ffffff'
  secondary-container: '#d5e0f7'
  on-secondary-container: '#586377'
  tertiary: '#00596f'
  on-tertiary: '#ffffff'
  tertiary-container: '#00738f'
  on-tertiary-container: '#cef0ff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdad6'
  primary-fixed-dim: '#ffb3ad'
  on-primary-fixed: '#410003'
  on-primary-fixed-variant: '#920212'
  secondary-fixed: '#d8e3fa'
  secondary-fixed-dim: '#bcc7dd'
  on-secondary-fixed: '#111c2c'
  on-secondary-fixed-variant: '#3c475a'
  tertiary-fixed: '#b8eaff'
  tertiary-fixed-dim: '#7fd1f1'
  on-tertiary-fixed: '#001f28'
  on-tertiary-fixed-variant: '#004d61'
  background: '#f9f9ff'
  on-background: '#121c2c'
  surface-variant: '#d9e3f9'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-sm:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.05em
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  unit: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  gutter: 24px
  margin-mobile: 16px
  margin-desktop: 40px
---

## Brand & Style

The design system is engineered for high-stakes enterprise environments, prioritizing reliability, efficiency, and authoritative clarity. The brand personality is professional and disciplined, designed to facilitate complex data entry and workflow management without cognitive fatigue.

The design style is **Corporate / Modern** with a focus on high-density information architecture. It leverages a structured grid, purposeful whitespace, and a high-contrast color palette to ensure that critical actions and data points are immediately identifiable. The aesthetic avoids unnecessary decoration, opting instead for functional elegance and rigorous alignment.

**Key Principles:**
- **Clarity over Expression:** Every visual element must serve a functional purpose.
- **Precision:** Tight alignment and consistent mathematical scaling across all components.
- **Trust:** A sober color palette anchored by a deep crimson to signal importance and professional vigor.

## Colors

The palette is anchored by a sophisticated Crimson Red, used strategically for primary actions and brand presence. This is balanced by a robust scale of "Slate" neutrals that provide the necessary contrast for enterprise-grade legibility.

- **Primary (#C53030):** Reserved for primary buttons, active states, and critical brand touchpoints.
- **Neutral (Slate Scale):** Uses a range from white (#FFFFFF) to deep charcoal (#1A202C) for backgrounds, borders, and text.
- **Semantic Colors:** Standardized Success, Warning, and Info colors are calibrated for high legibility against white backgrounds, specifically for form validation and status indicators.
- **Surface:** The default background is a very light gray (#F7FAFC) to differentiate card surfaces (#FFFFFF) from the application canvas.

## Typography

This design system utilizes **Inter** for all roles to ensure maximum readability and a technical, systematic feel. The hierarchy is established through significant weight shifts rather than excessive size variations.

- **Headings:** Use Semibold (600) or Bold (700) weights with tighter letter spacing to create a strong visual anchor.
- **Body:** Regular (400) weight is the standard for data and prose.
- **Labels:** Use Medium (500) or Semibold (600) at smaller scales for form labels and table headers to ensure they remain legible even at 12px.
- **Scalability:** Large headlines scale down for mobile viewports to prevent excessive wrapping in complex workflow screens.

## Layout & Spacing

The system uses a strict **8px base grid** (with 4px increments for micro-adjustments). The layout philosophy is a **Fluid-Fixed Hybrid**:

- **Sidebar:** Fixed width at 280px for navigation.
- **Content Area:** Fluid width with a max-width container of 1440px for optimal line lengths.
- **Grid:** A 12-column grid for desktop with 24px gutters.
- **Form Builder:** Uses a specific "Drafting Grid" with a 16px visual mesh to assist users in dragging and dropping components with precision.

Spacing should be generous to reduce visual noise in dense data environments. Use 32px (xl) to separate major sections and 16px (md) for internal card padding.

## Elevation & Depth

Depth is used sparingly to maintain a modern, flat aesthetic while providing necessary affordance for interactive layers.

- **Level 0 (Surface):** Background (#F7FAFC), no shadow. Used for the main application canvas.
- **Level 1 (Card):** White (#FFFFFF) with a thin 1px border (#E2E8F0) and a very soft ambient shadow (0px 1px 3px rgba(0,0,0,0.05)).
- **Level 2 (Hover/Active):** Slightly more pronounced shadow (0px 4px 6px rgba(0,0,0,0.07)) to indicate interactivity.
- **Level 3 (Modals/Popovers):** High-diffusion shadow (0px 10px 15px rgba(0,0,0,0.1)) with a dark backdrop overlay at 40% opacity.
- **Drag-and-Drop States:** Elements being moved should gain a Level 3 shadow and a slight 2-degree rotation to simulate physical lifting.

## Shapes

The shape language is **Soft (0.25rem / 4px)**. This provides a professional, geometric look that is slightly more approachable than sharp corners without feeling overly consumer-grade or playful.

- **Small Components:** Checkboxes, inputs, and small buttons use the base 4px radius.
- **Large Components:** Cards and modals use 8px (rounded-lg) for a more substantial structural feel.
- **Form Inputs:** Must maintain a consistent 4px radius to match button styles, creating a unified row appearance.

## Components

### Buttons
- **Primary:** Solid #C53030 background, white text. Bold weight.
- **Secondary:** Light gray background (#EDF2F7), dark gray text (#2D3748).
- **Ghost:** No background, #4A5568 text, shows #F7FAFC background on hover.

### Form Inputs
- **Default:** 1px border (#CBD5E0), 12px horizontal padding, 8px vertical padding.
- **Focus State:** 1px border #C53030 with a 3px soft outer glow in the same color at 20% opacity.
- **Validation:** Error states use #E53E3E for borders and helper text.

### Data Tables
- **Header:** Light gray background (#F7FAFC), uppercase bold labels, 1px bottom border.
- **Rows:** Alternating zebra striping (optional) or simple 1px bottom dividers. Hover state highlighting in #FFF5F5 (very light red).

### Form Builder
- **Draggable Items:** Subtle dashed border (#CBD5E0) when idle.
- **Drop Zone:** Primary Red dashed border with a light red tint background when an item is hovered over the target.
- **Controls:** Small floating toolbars for "Delete" or "Duplicate" actions using ghost button styles.

### Modals
- Centered on screen, max-width of 600px, 32px internal padding, primary action always positioned at the bottom right.