---
paths:
  - "app/src/main/**/*.kt"
---

# Kotlin and architecture

## Structure

- Package by feature: `ui/<feature>/` holds a feature's screen and ViewModel; shared components go in `ui/components/`.
- `data/` holds repositories and data sources. Add a `domain/` layer only when logic is shared between ViewModels.
- Add a layer, module or abstraction only when the current change needs it. One implementation needs no interface unless tests need to replace it.
- One public top-level concept per file, named after it.

## State and data flow

- Data flows down and events flow up: ViewModel → immutable `UiState` → screen → callbacks → ViewModel.
- A ViewModel exposes one `StateFlow<<Feature>UiState>`; a screen reads it with `collectAsStateWithLifecycle()`.
- UI never touches data sources; ViewModels never reference `Context`, views or composables.
- Model mutually exclusive states (loading, content, error) with a `sealed interface`, not with several flags.
- Persist what must survive process death; `rememberSaveable` or `SavedStateHandle` for what the user would expect back.

## Compose

- Split a screen into a stateful `<Feature>Route` that owns the ViewModel and a stateless `<Feature>Screen` (values in, callbacks out) once it has state. Previews and tests use the stateless one.
- Every composable that emits UI takes `modifier: Modifier = Modifier` as its first optional parameter and applies it to its root.
- Keep composition free of side effects and business logic; use `LaunchedEffect` and friends for effects.
- Use stable `key`s in lazy lists, and `derivedStateOf` for state computed from fast-changing state.

## Kotlin

- Official style, 120 columns, trailing commas in multi-line lists; prefer `val`, immutable collections and expression bodies.
- Full-word names; booleans read as questions (`isLoading`).
- Don't use `!!`; handle null explicitly. Catch specific exceptions, and never swallow `CancellationException`.
- Coroutines run in `viewModelScope` or `lifecycleScope`, never `GlobalScope`. Make blocking work main-safe with an injected dispatcher.
- Comments explain why, briefly; public declarations get a one-line KDoc. No commented-out code or debug logging.
- Fix warnings instead of suppressing; an unavoidable suppression is narrow with the reason on the same line.

## Security

- Never commit secrets, and never log personal data.
- Request the fewest permissions that work, at the moment they're needed, and handle denial.
