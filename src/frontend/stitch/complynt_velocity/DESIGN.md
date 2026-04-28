# Design System Specification: The Precision Architect

## 1. Overview & Creative North Star

This design system is anchored by a Creative North Star we call **"The Precision Architect."** 

In the world of enterprise compliance, the user is not just a "form-filler"; they are a high-stakes operator navigating a complex data landscape. We move away from the "Generic SaaS" look—characterized by flat white cards and harsh 1px borders—and toward a high-performance "cockpit" experience. 

The aesthetic is defined by **tonal depth, technical density, and optical clarity.** We achieve this through intentional asymmetry, glassmorphic layering that suggests transparency and "open books," and a rigorous typographic hierarchy that separates human-readable UI from machine-exact data.

---

## 2. Colors & Surface Logic

Our palette moves beyond blue as a "safe" choice, using it instead as a high-energy signal against a sophisticated, cool-gray foundation.

### The "No-Line" Rule
**Explicit Instruction:** Designers are prohibited from using 1px solid borders to define sections or containers. Boundaries must be established through:
1.  **Background Shifts:** Placing a `surface-container-lowest` (#ffffff) card on top of a `surface-container-low` (#f2f4f6) background.
2.  **Tonal Transitions:** Using the subtle delta between `surface` (#f7f9fb) and `surface-container` (#eceef0).

### Surface Hierarchy & Nesting
Think of the UI as a series of physical layers.
*   **Base:** `surface` (#f7f9fb) is your canvas.
*   **Nesting:** To highlight a specific module, use `surface-container-highest` (#e0e3e5) for the parent area and nest `surface-container-lowest` (#ffffff) elements inside it. This creates a "recessed" or "elevated" feel without a single stroke of a pen.

### The "Glass & Gradient" Rule
For the sidebar and floating overlays (modals/popovers), use a custom Glassmorphic stack:
*   **Fill:** `primary_container` (#0070f2) at 8% opacity.
*   **Backdrop:** `blur(20px) saturate(150%)`.
*   **Signature Texture:** Main CTAs should utilize a subtle linear gradient from `primary` (#0058c2) to `primary_container` (#0070f2) at a 135-degree angle to provide a "lit from within" professional glow.

---

## 3. Typography: The Dual-Tone System

We employ two distinct typefaces to separate the "interface" from the "intelligence."

*   **IBM Plex Sans (UI Engine):** Used for all labels, headings, and instructional text. It conveys a modern, industrial-grade reliability.
    *   *Headline-LG:* Use for page titles to establish a bold, editorial anchor.
    *   *Body-MD:* The workhorse for standard interaction.
*   **IBM Plex Mono (Data Engine):** Used for IDs, timestamps, compliance codes, and numerical values. 
    *   By shifting data into a monospaced font, we signal to the user that this information is "system-generated" and "unalterable," enhancing the feeling of precision and auditability.

**Hierarchy Note:** Use `on_surface_variant` (#414754) for secondary labels (Label-SM) to create a clear "read-first, act-second" visual flow.

---

## 4. Elevation & Depth

We eschew "drop shadows" in favor of **Ambient Occlusion** and **Tonal Layering.**

*   **The Layering Principle:** Depth is achieved by "stacking" the surface-container tiers. Never place two containers of the same hex value next to each other.
*   **Ambient Shadows:** If an element must float (e.g., a dropdown or a "Save" bar), use an extra-diffused shadow: `box-shadow: 0 12px 40px rgba(0, 88, 194, 0.06)`. Note the tint: we use a percentage of the `primary` color rather than black to keep the shadows "cool" and integrated.
*   **The "Ghost Border" Fallback:** In high-density data views where tonal shifts aren't enough, use a Ghost Border: `outline_variant` (#c1c6d7) at **15% opacity**. It should be felt, not seen.

---

## 5. Components

### Sidebar (The Signature Element)
*   **Background:** Glassmorphic blur as defined in Section 2.
*   **Active State:** Use a "Vibrant Pill." An active item uses `primary_container` as a background with a `primary` vertical "light bar" (3px wide) on the left edge.
*   **Corners:** `md` (12px) for the active selection indicators.

### Buttons
*   **Primary:** Gradient fill (Primary to Primary Container). Corner radius: `DEFAULT` (8px).
*   **Secondary:** No fill. Ghost Border (15% opacity) with `primary` text.
*   **States:** On hover, increase the opacity of the gradient; never change the hue.

### High-Performance Cards
*   **Rule:** Forbid divider lines. 
*   **Spacing:** Use `spacing.8` (1.75rem) to separate internal content blocks.
*   **Header:** Use a subtle background shift to `surface_container_low` for the card header to distinguish it from the card body.

### Technical Inputs
*   **Default:** `surface_container_lowest` background.
*   **Focus:** Instead of a thick border, use a 2px outer glow of `primary` at 20% opacity and change the "label" text to `primary`.
*   **Data Entry:** All user-inputted numbers must render in **IBM Plex Mono**.

---

## 6. Do’s and Don’ts

### Do:
*   **Do** use asymmetrical layouts. A sidebar on the left, a wide content area, and a slim "contextual inspector" on the right creates a professional, "tool-first" feel.
*   **Do** lean into `md` (12px) rounding for large containers and `DEFAULT` (8px) for interactive elements. This creates a nested "nested curve" harmony.
*   **Do** use `tertiary` (#a23d00) sparingly for "Warning" states. It provides a sophisticated alternative to standard bright oranges.

### Don’t:
*   **Don’t** use 100% black text. Use `on_surface` (#191c1e) to maintain the "cool" enterprise tone.
*   **Don’t** use standard dividers. If you feel the need to separate two items, increase the spacing from `4` (0.9rem) to `6` (1.3rem) or shift the background color.
*   **Don’t** allow "floating" cards to have sharp corners. All elevated surfaces must adhere to the `md` (12px) or `lg` (16px) rounding to feel premium.