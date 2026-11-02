---
paths:
  - "app/src/main/**/ui/**"
  - "app/src/main/res/**"
---

# UI and design

- Use Material 3 components before custom ones, and match how the existing screens look and behave.
- **Color:** `MaterialTheme.colorScheme` roles only, never hardcoded colors, and always in pairs (`surface` with `onSurface`).
- **Type:** `MaterialTheme.typography` roles only. Text sizes are in `sp`, so they follow the user's font scale.
- **Spacing:** multiples of 4 dp, usually 8, 16 or 24, with the same values for the same purpose across screens.
- **Layout:** one clear primary action per screen. Build for phones first, and don't let content overflow on small screens, in landscape or at a large font scale.
- **Edge to edge:** the app draws behind the system bars, so handle insets with `Scaffold` or `safeDrawing`; scrolling content keeps clear of them through `contentPadding`.
- **States:** every screen that loads data handles loading, empty and error, and errors say what the user can do next.
- **Feedback:** every tap responds visibly. Use Material motion, kept short; an animation must not delay the user.

## Accessibility

- Touch targets are at least 48 × 48 dp.
- Icons that convey meaning have a `contentDescription` from `strings.xml`; decorative ones pass `null`.
- Color is never the only signal, and text keeps its contrast in both themes.
- Group related elements with `Modifier.semantics(mergeDescendants = true)` when they read as one item.

## Resources and previews

- All user-visible text lives in `strings.xml`, with `plurals` for counts. Names are `snake_case`, prefixed by feature (`home_title`).
- Icons are vector drawables named `ic_<name>.xml`.
- Each screen has a `@PreviewLightDark` of the stateless composable, with one more per meaningful state.
