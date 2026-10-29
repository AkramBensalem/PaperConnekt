# PaperConnekt

**PaperConnekt is a Kotlin Multiplatform research workspace for reading,
organising, connecting, and discussing scientific literature.**

The project grew out of the daily needs of a PhD researcher: finding relevant
papers, keeping notes close to the source, moving between ideas, and turning a
large collection of references into a research direction. PaperConnekt brings
those activities into one focused workspace instead of scattering them across a
PDF reader, a reference manager, an IDE, and disconnected AI tools.

The project is being prepared for the
[KotlinConf Student Contest](https://kotlinconf.com/contest/), with a strong
focus on Kotlin Multiplatform, thoughtful product design, and practical value
for students and researchers.

## What PaperConnekt does

### Research dashboard

The dashboard is the starting point for research. It provides a focused view of
saved papers, manual paper entry, local search, and optional discovery through
OpenAlex. The workflow is designed to keep the researcher close to the
literature rather than turning the app into a generic document manager.

### AI research assistance

PaperConnekt keeps assistant conversations alongside the research workspace.
Users can maintain a history of questions, relate discussions to papers, and
open a paper from the assistant flow in the PDF workspace. Provider
credentials are optional and managed locally, so the user can choose whether
and when to connect an AI provider.

### Research graphs

Research graphs make relationships explicit. A graph can collect papers around
a question, topic, or thesis chapter, while paper-to-paper connections help
surface the structure behind a literature review.

### Personal research library

The library provides a local hierarchy for folders and papers, inspired by the
organisation patterns researchers know from Zotero. It is useful with or
without a Zotero connection, so the core workflow remains available while
travelling, teaching, or working offline.

The library workspace now presents collections as a navigable nested tree with
folder counts, search, unfiled/recent views, and collection actions. Each item
can be completed, moved, opened in the reading workspace when a PDF is
attached, or edited in place. Item details support a local PDF path, metadata,
and abstract/reading notes; the attached path is intentionally local-first and
does not upload files or require an account.

### IDE and editor plugins

Research does not stop when a researcher starts writing. PaperConnekt also
provides native plugin adapters for IntelliJ IDEA and VS Code. The plugins are
intended to make the researcher’s preferred writing environment a natural
extension of the workspace: a place to find papers, consult notes, and move
between literature and LaTeX or source code without losing context.

The plugin architecture is deliberately shared. A Kotlin `common` plugin
module contains reusable indexing and session behaviour, while thin native
adapters connect that behaviour to IntelliJ IDEA and VS Code. The app and
plugins also use the same Kotlin domain models and storage contracts. This
avoids rebuilding the research model for every host and demonstrates how
Kotlin Multiplatform can share meaningful product logic across an application
and its native integrations.

## Product principles

## Platform menus

Desktop has a native **File / View / Help / About** menu bar. Add a paper,
open Settings, switch research views or themes, toggle fullscreen, and open the
local workspace folder in the system file manager. Both Quit and window-close
use the PDF workspace's unsaved-edit confirmation.

Use **Command** on macOS or **Ctrl** on Windows/Linux with **N** to add a paper,
**,** for Settings, **1-4** for Dashboard/Library/Graph/Assistant, and **Q** to
quit. **F11** toggles fullscreen. Workspace actions become available after
initialization and onboarding; Help and About remain available independently.

Android keeps its adaptive bottom navigation/navigation rail and adds a
Material overflow **Menu** for secondary actions. It does not expose desktop
filesystem paths, fullscreen controls, or Quit. PDF attachments continue to
use Android's system document picker.

Getting started, About, developer/contact information, acknowledgements,
issue reporting, and release links share the same content on both platforms.
External links open only on request. Updates link to GitHub releases; there
is no automatic update check or installer. Opening the desktop workspace
folder does not change its save location or migrate data.

## Product principles

- **Researcher-centred:** every feature supports reading, organising,
  connecting, or writing about literature.
- **Local-first:** the essential workspace is useful on the device and does
  not depend on an account or a continuously available service.
- **Private by design:** provider keys are stored through platform-specific
  secure storage abstractions and are only used when the researcher enables an
  integration.
- **Native where it matters:** the shared research model is written in Kotlin,
  while host-specific UI and integrations use the conventions of each
  platform.
- **Composable:** optional providers such as OpenAlex, Zotero, and AI services
  extend the workspace without defining its core experience.

## Architecture

```text
app/android     app/desktop     future app/web
        \          |             /
             app/composeApp
                                 |
                                 +--> shared/storage ----> shared/domain
                                 |
                                 +--> shared/secrets
                                 |
                                 +--> plugin/native-api
                                           |
                                           +--> plugin/native-plugins/common
                                                  |             |
                                                  v             v
                                                vscode         idea
             ```

| Layer | Responsibility |
| --- | --- |
| `shared/domain` | Serializable papers, graphs, library entries, assistant sessions, settings, and navigation contracts |
| `shared/storage` | Local research-store interfaces, local data access, and optional paper discovery providers |
| `shared/secrets` | A common contract for storing provider credentials through platform-secure implementations |
| `plugin/native-api` | The shared contract for native editor and IDE integrations |
| `plugin/native-plugins/common` | Kotlin behaviour shared by the native plugins, including paper indexing and plugin sessions |
| `plugin/native-plugins/vscode` | Native VS Code adapter |
| `plugin/native-plugins/idea` | Native IntelliJ IDEA adapter |
| `app/composeApp` | Shared Compose Multiplatform UI and application flow |
| `app/android` | Android launcher, manifest, and Android-native integrations |
| `app/desktop` | JVM desktop launcher, window configuration, and desktop integrations |

The dependency direction keeps product logic and UI in shared Kotlin modules.
Each platform module owns only its launcher and native boundary, so Android and
desktop can evolve independently while a future web module can reuse the same
Compose application flow.

## App and plugin icons

The PaperConnekt mark combines a folded-paper **P** with a three-node research
graph on a rounded blue tile. The letter represents papers; the linked nodes
represent connections between research ideas.
Its gradient uses `#3B82F6`, the app's primary blue `#2457A6`, and `#173D80`.
`branding/paperconnekt.svg` is the editable master; all platform exports share
the same glyph and palette.

- Android owns adaptive launcher layers, round/legacy density exports, and an
  Android 13 monochrome themed icon in `app/android/src/main/res`.
- Desktop owns its window icon and Linux PNG, Windows multi-size ICO, and
  macOS ICNS packaging assets in `app/desktop/src/main/resources/icons`.
  Apple packaging still requires macOS validation.
- The shared Compose navigation rail uses a PNG export, supported on both
  Android and desktop.
- Native plugin resources include IntelliJ plugin-list and light/dark
  tool-window SVGs, plus VS Code listing PNG and monochrome activity-bar SVG.
  The adapters do not yet have installable host manifests; see the native
  plugin README for their intended wiring.

To re-export after editing the master, run
`python3 tools/generate_brand_icons.py` with Pillow and CairoSVG available.
This only writes icon resources; it does not run Gradle.
The mark is custom vector artwork, not a generic chain-link glyph; its design
notes are in `branding/NOTICE.txt`.

## Technology

| Area | Technology |
| --- | --- |
| Language | Kotlin 2.1.21 |
| Multiplatform | Kotlin Multiplatform |
| User interface | Compose Multiplatform and Material 3 |
| Android | Android SDK 35, minimum SDK 26 |
| Desktop | Compose Desktop on JVM 17 |
| Networking | Ktor Client, used by optional discovery integrations |
| Data format | Kotlinx Serialization |
| Concurrency | Kotlin Coroutines |
| Paper discovery | OpenAlex integration, with local workflows available independently |
| Secret storage | Android Keystore on Android; platform preferences on desktop |
| Native integrations | Kotlin/JVM adapters for IntelliJ IDEA and VS Code |

## Platform status

| Platform | Status |
| --- | --- |
| Android | Primary mobile target and actively developed |
| Desktop JVM | Supported Compose Desktop target for research and writing workflows |
| Windows/Linux desktop | Available through the JVM desktop target, subject to local packaging |
| macOS | Planned desktop validation target; current development resources do not include a macOS device, so Apple-specific packaging and release validation remain to be completed |
| iOS and iPadOS | Planned KMP targets; implementation can follow the shared Kotlin foundation when Apple hardware and signing access are available |

The Apple targets are part of the longer-term platform direction. Their current
validation status reflects development-resource constraints rather than a
product limitation in the shared architecture.

## Getting started

Use JDK 17 or newer and open the project in IntelliJ IDEA or Android Studio with
Kotlin Multiplatform support. The project keeps platform adapters small and
places reusable research logic in common Kotlin source sets.

## License

See the repository license file for the applicable terms.
