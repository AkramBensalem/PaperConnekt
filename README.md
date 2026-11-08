# PaperConnekt

> A local-first research workspace for reading, organising, connecting, and
> discussing scientific literature.

PaperConnekt brings papers, notes, graphs, reading context, and optional AI
assistance into one focused workspace. It is built with Kotlin Multiplatform
so the core research model can be shared across the app and native editor
integrations.

The project is being prepared for the
[KotlinConf Student Contest](https://kotlinconf.com/contest/).

## What you can do

| Area | In a sentence |
| --- | --- |
| **Library** | Save papers, organise them into nested collections, attach local PDFs, and keep reading notes. |
| **PDF workspace** | Read and annotate imported PDFs without uploading them or requiring an account. |
| **Research graphs** | Connect papers around a question, topic, or thesis chapter. |
| **Assistant** | Keep research conversations beside papers, with optional provider integrations. |
| **Discovery** | Search OpenAlex when you choose; local papers remain available offline. |
| **Editor integrations** | Open the same local workspace from IntelliJ IDEA and VS Code. |

## Project status

PaperConnekt is an active contest project. The table below describes the
current direction rather than promising production support.

| Area | Status |
| --- | --- |
| Shared domain, storage, and secrets contracts | Available |
| Android application | Available; primary mobile target |
| Desktop JVM application | Available; active research and writing workflow |
| Web/Wasm application and local static host | Available; evolving |
| IntelliJ IDEA plugin | Available; local workspace integration |
| VS Code extension | Available; read-only workspace navigation |
| iOS/iPadOS | Planned |
| macOS packaging validation | Planned; requires Apple hardware |

For the feature roadmap, see [the branch plan](docs/branch-plan.md).

## Technology

| Concern | Libraries and tools |
| --- | --- |
| Language and build | Kotlin, Kotlin Multiplatform, Gradle |
| UI | Compose Multiplatform, Material 3 |
| Android | Android SDK, AndroidX Activity and Lifecycle |
| Desktop | Compose Desktop, JVM 17 |
| Web | Kotlin/Wasm, Compose/Wasm, Ktor static host |
| Networking | Ktor Client |
| Data and models | Kotlinx Serialization, Kotlinx DateTime |
| Concurrency | Kotlin Coroutines |
| Research agent | Koog (optional provider-backed assistance) |
| PDF and text | PDFium/PDFBox, Markdown renderer, JLaTeXMath |
| Native integrations | IntelliJ Platform, Jewel, VS Code extension API |

## Platforms

| Target | Launcher/module | Notes |
| --- | --- | --- |
| Android | `app/android` | Minimum SDK 26; uses the Android sandbox and document picker |
| Desktop JVM | `app/desktop` | JDK 17; Windows and Linux packaging is supported |
| Web/Wasm | `app/web` + `app/webServer` | Browser-local storage; can be served with Docker |
| IntelliJ IDEA | `plugin/idea` | Runs in an IntelliJ sandbox |
| VS Code | `plugin/vscode` | Reads the shared local workspace |

## Quick start

Choose the option that matches your goal:

- **Just run the app:** install Docker Desktop (Windows) or Docker Engine
  (Linux), then follow [Run the web app with Docker](docs/installation.md#option-a-run-the-web-app-with-docker).
- **Run the native desktop app:** follow
  [Build and run from source](docs/installation.md#option-b-build-and-run-from-source).
- **Develop or package the project:** follow
  [Developer setup and builds](docs/installation.md#developer-setup-and-builds).

The web option is currently the simplest way to run PaperConnekt on Windows or
Linux. There is not yet a published native Windows or Linux installer.

### Run the web app with Docker

After installing Docker, the quickest start is:

```bash
git clone https://github.com/AkramBensalem/PaperConnekt.git
cd PaperConnekt
docker compose up --build
```

Open <http://localhost:8080>. Stop the server with `docker compose down`.
The app stores workspace data in the browser; the container is only a static
asset host.

### Run the other targets

The main Gradle entry points are:

```bash
./gradlew :app:desktop:run
./gradlew :app:android:installDebug
./gradlew :plugin:idea:runIde
```

Android requires a connected device or running emulator. See
[Development](docs/development.md) for build variants, the VS Code extension,
and contributor workflow.

## Documentation

Start at the [documentation hub](docs/README.md):

| Guide | Purpose |
| --- | --- |
| [Installation](docs/installation.md) | Prerequisites, cloning, running each target, and common setup issues |
| [Development](docs/development.md) | Module map, useful Gradle tasks, plugin workflow, and contribution conventions |
| [Architecture](docs/architecture.md) | Shared contracts, local-first storage, PDF handling, and feature boundaries |
| [Web architecture](docs/web-architecture.md) | Browser storage, routing, Wasm hosting, and the web implementation plan |
| [AI agent architecture](docs/ai-agent-architecture.md) | Paper retrieval, citations, embeddings, and MCP tools |
| [DevOps and GitHub Actions](docs/devops.md) | Remote builds, artifacts, and the release/deployment plan |
| [Feature roadmap](docs/branch-plan.md) | Contest feature slices and project milestones |
| [Native plugin notes](plugin/native-plugins/README.md) | IntelliJ and VS Code adapters, shared workspace behavior, and packaging |

## Design principles

- **Local-first:** reading, organising, notes, and saved research work without
  an account or a required backend.
- **Researcher-centred:** features support reading, connecting, and writing
  about literature rather than adding generic productivity overhead.
- **Private by design:** provider credentials stay behind platform-specific
  secure-storage abstractions and are used only when enabled.
- **Shared where useful, native where necessary:** domain logic and contracts
  are shared; launchers and host integrations follow each platform's
  conventions.

## Data and privacy

The contest application stores the workspace locally. Desktop data defaults to
`~/.paperconnekt-contest`; Android uses its private app storage; the web target
uses browser storage. Imported PDFs are copied into managed local storage.
Provider credentials are not written into the workspace snapshot.

Optional OpenAlex, Zotero, and AI integrations require explicit configuration.
They do not turn the application into a mandatory cloud service.

## Contributing

Small, focused changes are easiest to review. Before opening a pull request,
read [Development](docs/development.md), keep product models in shared modules,
and keep platform launchers in their platform modules. Do not add credentials,
generated local data, or machine-specific configuration.

## License

See the repository license file for the applicable terms.
