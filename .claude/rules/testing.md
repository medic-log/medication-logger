---
paths:
  - "app/src/test/**"
---

# Tests

- Test behavior through public APIs, not implementation details. One behavior per test, named in backticks: `` `stop resets the timer` ``.
- Cover ViewModel and data logic first, including the error paths. Don't test framework behavior or trivial getters.
- Prefer fakes over mocks. Inject clocks and dispatchers, and use `runTest` with virtual time, never `Thread.sleep`.
- Use Robolectric only when a test needs Android classes or resources; plain JUnit is faster.
- Test screens and user flows with `createAndroidComposeRule`, which runs on the JVM under Robolectric. Find nodes by text from `strings.xml` or by semantics, and assert what the user sees.
- A failing test is either a bug in the code or a wrong expectation: work out which before changing either.
