---
paths:
  - "**/*.gradle.kts"
  - "gradle/libs.versions.toml"
---

# Build and dependencies

- Declare every version in `gradle/libs.versions.toml`; Compose artifacts take theirs from the BOM.
- Ask before adding a dependency. Prefer the platform or what's already in the catalog, and say what a new one is for.
- Don't upgrade versions unasked, and remove what a change leaves unused.
- Never put signing values or other secrets in build files; they come from the environment.
