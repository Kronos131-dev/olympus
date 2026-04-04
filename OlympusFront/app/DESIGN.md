# Design System Strategy: Monumental Discipline

## 1. Overview & Creative North Star
**Creative North Star: "The Digital Pantheon"**

This design system rejects the ephemeral "flatness" of modern SaaS in favor of something eternal, weightful, and authoritative. We are not building a tracker; we are building a digital monument to the user’s personal conquest.

To break the "template" look, we utilize **Imperial Asymmetry**. This means layouts should feel like classical architecture—structured but expressive. We avoid perfectly centered grids in favor of heroic, off-center focal points, overlapping serif typography that "bleeds" across container boundaries, and high-contrast tonal shifts. Every screen should feel like a stone-carved inscription, where the negative space is as powerful as the content itself.

## 2. Colors & Surface Philosophy
The palette is rooted in the "Triad of Triumph": Imperial Purple, Tyrian Purple, and Rich Gold, set against the cold, unyielding depth of Marble White and Deep Onyx.

* **The "No-Line" Rule:** We do not use 1px solid borders to define sections. We define space through **Tonal Carving**. A section is not "boxed in"; it exists because its background shifts from `surface` (#121414) to `surface_container_low` (#1a1c1c).
* **Surface Hierarchy & Nesting:** Treat the UI as a series of stone slabs. A `surface_container_lowest` (#0c0f0f) element should be used for background utility, while a `secondary_container` (#af8d11) should be reserved for the most prestigious calls to action.
* **The "Glass & Gradient" Rule:** To avoid a "costume" look and maintain a high-end digital feel, use Glassmorphism for floating action buttons or navigation bars. Use `surface_variant` (#333535) with a 60% opacity and a 20px backdrop blur.
* **Signature Textures:** Apply a subtle, high-frequency noise texture (simulating marble grain) to `surface` containers. Use a linear gradient on Gold elements transitioning from `secondary` (#e9c349) to `secondary_container` (#af8d11) at a 45-degree angle to simulate the sheen of real metal.

## 3. Typography: The Authoritative Script
Typography is our primary tool for conveying "Veni, Vidi, Vici."

* **Display & Headlines (Newsreader):** This is our "Monumental" face. Use `display-lg` for daily goals and "victory" states. It should feel etched into the screen. Increase letter-spacing slightly (+2-5%) to mimic Roman lapidary inscriptions.
* **Body & Labels (Manrope):** To maintain "High Performance" readability, we use a clean, geometric sans-serif for functional data. This creates a sophisticated tension between the ancient and the modern.
* **Editorial Hierarchy:** Large, serif headlines should often overlap image containers or use `primary_container` (#5d3fd3) as a background "highlight" to emphasize the "Noble" brand personality.

## 4. Elevation & Depth
We move away from the "floating card" aesthetic of 2015. Depth in this system is about **Material Weight.**

* **The Layering Principle:** Stack `surface_container` tiers to create hierarchy. A workout card (`surface_container_high`) sits on a workout category section (`surface_container_low`). No shadows are needed; the shift in tone provides the "lift."
* **Ambient Shadows:** If a floating element is required (e.g., a modal), use a wide, diffused shadow.
* *Shadow:* `0px 24px 48px rgba(0, 0, 0, 0.4)`. The shadow color is never grey; it is a darkened tint of the background surface.
* **The "Ghost Border" Fallback:** For secondary inputs or subtle dividers, use `outline_variant` (#484554) at 15% opacity. If a border is gold, use the `secondary` token but only on one or two sides (e.g., a left-accent border) to suggest a gilded edge rather than a box.

## 5. Components

* **Buttons:**
* *Primary (The Laurels):* Solid `secondary_container` (#af8d11) with `on_secondary` (#3c2f00) text. Sharp corners (`0px` roundedness).
* *Secondary (The Senate):* Transparent background with a `secondary` gold "Ghost Border" (20% opacity).
* **Chips:** Used for muscle groups. Use `surface_container_highest` with `label-md` typography. No border.
* **Cards & Lists:** **Strictly forbid divider lines.** Use `1.4rem` (`spacing-4`) or `2rem` (`spacing-6`) of vertical whitespace to separate items. A "List Item" is simply a change in `surface_container` tone on hover/tap.
* **Input Fields:** Use `surface_container_lowest` for the field body. The label (`label-sm`) sits above in `secondary` gold. On focus, the bottom border animates to a 2px `secondary` gold line.
* **The "Heroic" Progress Ring:** Instead of a thin line, use a thick, `tertiary_container` ring that fills with a `secondary` (Gold) gradient. It should feel like a laurel wreath encircling the user's data.

## 6. Do’s and Don’ts

**Do:**
* Use `0px` border radius everywhere. Sharp edges convey discipline and strength.
* Use `surface_bright` to highlight active states in navigation.
* Pair epic, desaturated photography (statues, athletes in high-contrast lighting) with vibrant Purple and Gold accents.
* Let text breathe. Use `spacing-10` (3.5rem) for section margins to create a gallery-like feel.

**Don’t:**
* **Don't use rounded corners.** It softens the brand and breaks the "Monumental" aesthetic.
* **Don't use standard icons.** Use custom, thin-stroke gold icons that resemble classical motifs (swords for strength, wings for cardio).
* **Don't use pure white (#FFFFFF).** Always use `inverse_surface` (#e2e2e2) or `surface_bright` to maintain the "Marble" tonality.
* **Don't crowd the screen.** If a screen feels busy, increase the spacing tokens and remove decorative elements. True power is found in restraint.