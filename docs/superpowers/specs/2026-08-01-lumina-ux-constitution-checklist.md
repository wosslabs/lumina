# Lumina UX Constitution — PR Checklist

Use on every PR that touches `lumina-web` client/CSS or shell authoring APIs. Derived from `2026-08-01-lumina-ux-hard-reset-design.md` §4.

## Semantics
- [ ] Landmarks present where applicable (`banner`, `navigation`, `main`, `complementary`/`contentinfo`)
- [ ] One H1 per view (`ui.title`); header context is not an H1
- [ ] Interactive controls have accessible names (visible label or `aria-label`)

## Keyboard & focus
- [ ] All interactive controls reachable and operable by keyboard
- [ ] Visible `:focus-visible` rings on interactive elements
- [ ] No accidental focus traps

## Density & targets
- [ ] Spacing follows 4/8px scale
- [ ] Primary controls ≥ ~40px / 2.5rem min-height hit target

## Chrome & color
- [ ] Quiet neutral shell; accent reserved for primary actions / current nav
- [ ] Light and dark tokens remain WCAG AA for text/icons

## Motion
- [ ] Motion is short and purposeful
- [ ] `prefers-reduced-motion: reduce` disables/non-essential animation

## Forms
- [ ] Labels visible for inputs
- [ ] `help` (when present) exposed as accessible description

## Thin client
- [ ] No requirement for author-written HTML/CSS/JS
