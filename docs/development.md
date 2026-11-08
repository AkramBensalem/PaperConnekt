# Development

PaperConnekt is a Kotlin Multiplatform project. Keep reusable product logic
and contracts shared, and keep platform launchers and native integrations at
their platform boundary.

## Repository layout

| Path | Responsibility |
| --- | --- |
| `app/composeApp` | Shared Compose UI and application flow |
| `app/android` | Android launcher, manifest, and Android-native adapters |
| `app/desktop` | JVM desktop launcher, windowing, and desktop adapters |
| `app/web` | Wasm launcher and browser adapters |
| `app/webServer` | Optional Ktor static asset host |
| `shared/domain` | Serializable papers, graphs, library, assistant, and settings models |
| `shared/storage` | Local persistence and optional provider contracts |
| `shared/secrets` | Secure credential storage contract |
| `shared/knowledge` | Paper chunking, indexing, retrieval, and citations |
| `shared/mcp` | MCP server core and research tools |
| `plugin/native-api` | Shared native plugin contract |
| `plugin/native-plugins/common` | Plugin-only shared behavior |
| `plugin/native-plugins/idea` | IntelliJ adapter implementation |
| `plugin/native-plugins/vscode` | VS Code adapter implementation |
| `plugin/idea` | Installable IntelliJ Platform plugin |
| `plugin/vscode` | Installable VS Code extension |

For dependency direction and persistence guarantees, use
[Architecture](architecture.md) as the source of truth.

## Useful commands

Run commands from the repository root:

```bash
# Start a target
./gradlew :app:desktop:run
./gradlew :app:android:installDebug
./gradlew :plugin:idea:runIde

# Build the web distribution
./gradlew :app:web:wasmJsBrowserDistribution

# Build the web server distribution
./gradlew :app:webServer:installDist

# Package the VS Code extension
cd plugin/vscode && npm run package
```

The web Docker image combines the Wasm distribution, PDFium runtime assets,
and Ktor host. See [Installation](installation.md) for the user-facing setup.

## Remote builds

GitHub Actions builds the project remotely for pull requests and pushes. Read
[DevOps and GitHub Actions](devops.md) for the workflow, downloadable
artifacts, and the planned release/deployment phases.

## Contribution conventions

- Keep changes focused and preserve unrelated working-tree changes.
- Put models and contracts in `shared/`; do not duplicate them in platform
  modules.
- Keep Android, desktop, web, IntelliJ, and VS Code entry points in their own
  modules.
- Preserve local-first behavior. Do not add a mandatory backend, sign-in flow,
  or Supabase dependency.
- Keep provider integrations optional and keep secrets out of ordinary
  preferences and workspace snapshots.
- Update the relevant documentation when architecture or user-facing behavior
  changes.
- Do not commit generated build output, local workspace data, credentials, or
  machine-specific configuration.

## Documentation workflow

The root [README](../README.md) is the short project overview. This directory
contains the deeper guides. When adding documentation:

1. Choose a focused topic and place it under `docs/`.
2. Add it to the [documentation hub](README.md).
3. Link it from the root README when it is useful to most contributors.
4. Prefer tables, short sections, and copy-pasteable commands.

## Working with local data

Desktop and Android writes are versioned and local. A malformed snapshot is
preserved for recovery rather than silently replaced. Do not edit
`workspace.json` while the application is running. When testing integrations,
use a separate data directory so production-like local data is not changed.

The IntelliJ and VS Code integrations can read the same workspace as the
desktop app. Avoid simultaneous writes from multiple applications; refresh
after an external change before editing.
