# PaperConnekt contest repository instructions

## Validation policy

- Do not run Gradle synchronization, Gradle builds, tests, lint, `check`, or other
  verification commands by default.
- The user runs validation unless they explicitly ask for a specific validation
  command or explicitly authorize builds and tests for the current task.
- Static inspection is allowed: read files, search the repository, inspect Git
  state, and use non-build checks such as `git diff --check`.
- When explicit validation is requested, run only the requested or smallest
  relevant command and report the exact command and result.
- Never claim that a build, test, or synchronization succeeded unless it was
  actually run.

## Change workflow

- Work in the contest repository, not the startup repository
  `PaperConnekt-v2`.
- Keep feature work isolated in a descriptive `contest/*` branch.
- Do not create commits, push branches, or open pull requests unless the user
  explicitly requests that action.
- Preserve unrelated working-tree changes and never reset, stash, or discard
  user work without explicit permission.
- Read the relevant existing code and documentation before editing.
- Prefer small, focused changes that can be reviewed and merged independently.

## Architecture

- Keep product models and contracts in the shared KMP modules.
- Keep the application UI in `app/composeApp`.
- Keep plugin-only shared behavior in
  `plugin/native-plugins/common`.
- Keep IntelliJ IDEA and VS Code integrations as thin native adapters.
- Reuse shared Kotlin models and storage contracts between the application and
  plugins instead of duplicating representations.
- Preserve the local-first, researcher-centred product direction and optional
  provider integrations.

## Dependencies and secrets

- Prefer Kotlin Multiplatform and native platform APIs.
- Do not add a remote backend, Supabase dependency, or mandatory sign-in flow
  to the contest application.
- Do not commit credentials, provider keys, generated local data, or machine
  specific configuration.
- Update directly related documentation when architecture or user-facing
  behavior changes.
