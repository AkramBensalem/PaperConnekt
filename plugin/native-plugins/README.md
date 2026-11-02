# Native plugin modules

The contest edition keeps native plugin code in three focused Kotlin modules:

```text
plugin/native-plugins/
├── common/   shared plugin-only index and session behavior
├── vscode/   VS Code native adapter
└── idea/     IntelliJ IDEA native adapter
```

`common` is shared between the two native plugins but is intentionally separate
from the Compose app UI. Cross-product contracts remain in:

- `shared/domain` for papers, graphs, library entries, and assistant sessions
- `shared/storage` for local persistence contracts
- `plugin/native-api` for the native plugin interface

This keeps the shared behavior native while allowing the app and host adapters
to exchange the same Kotlin data models and plugin contract. The VS Code host
surface remains a thin JavaScript adapter, while its data contract is the same
local workspace file consumed by the Kotlin IntelliJ adapter.

## Brand resources

Both adapters carry exports of `branding/paperconnekt.svg`. Regenerate them
with `python3 tools/generate_brand_icons.py` from the repository root.

| Host surface | Resource |
| --- | --- |
| IntelliJ plugin listing | `idea/src/main/resources/META-INF/pluginIcon.svg` and `pluginIcon_dark.svg` (40 px) |
| IntelliJ tool window/action | `idea/src/main/resources/icons/paperconnekt.svg` and `paperconnekt_dark.svg` (16 px, theme-specific blue) |
| VS Code extension listing | `vscode/src/main/resources/icons/paperconnekt.png` (128 px) |
| VS Code activity bar | `vscode/src/main/resources/icons/paperconnekt-activity.svg` (24 px, monochrome for host tinting) |

The host adapters are packaged in:

- `plugin/idea`, an IntelliJ Platform plugin with a docked PaperConnekt tool
  window and a Tools-menu action.
- `plugin/vscode`, a VS Code extension with a PaperConnekt activity-bar view,
  shared-workspace paper navigation, refresh, and local PDF opening.

Both adapters read the same local `workspace.json` used by the desktop app.
The VS Code adapter remains read-only. The IntelliJ host also saves graph edits,
node positions, imported PDF attachments, last-opened timestamps, and reading
positions through the shared storage contracts. It reloads the workspace before
each mutation and rejects a detected external change rather than replacing it
with stale data. Refresh before continuing after a conflict; avoid editing the
same workspace simultaneously in two applications.

## IntelliJ native research workspace

`plugin/idea` uses the Jewel/Compose approach from the reference `idea-native`
plugin. Its reader, annotation menus, floating assistant, Idea Board, Markdown
adapter, and keyboard-shortcut code were imported from that implementation and
adapted to local contest papers rather than a signed-in Zotero account.

The graph canvas and force-layout engine are compiled directly from the app's
`legacygraph` sources, together with the app's graph-filtering algorithms. Native
controls provide saved graphs, local/OpenAlex seed selection, citation/reference
expansion, manual connections, paper selection, filtering, and persisted layouts.
Remote searches and expansion run only when explicitly requested.

The PDF reader uses the reference plugin's PDFBox rasterizer and text geometry,
not an external application or Chromium's PDF viewer. Its controller, local
annotation persistence, OCR, paper-agent tools, chat history, and Idea Board
storage are source-shared with the contest application. Managed attachments,
annotation keys, and reading-position keys match the desktop app.

Selected common sources are compiled against **IDEA's Compose runtime**. The
plugin does not depend on the precompiled `app:composeApp` artifact or package a
second Compose/Skiko runtime. JVM store-factory bindings are generated from the
existing common/desktop sources in the IDEA module's build directory. This
avoids both duplicate runtime classes and calls compiled against a different
Compose ABI, without changing Android, Desktop, or web builds.

The adaptation is local-first: there is no account gate, Supabase service,
cloud-sync screen, or cloud speech service. Reading, graph layout, annotations,
and notes need no provider credentials. Optional OpenAI, Mistral, and OpenAlex
keys can be set in the workspace menu's **Provider settings** and stay in
IntelliJ Password Safe. Keys are not automatically copied from the desktop app.
AI chat sends the chosen context to the selected provider; requesting Mistral
OCR sends the PDF to Mistral. The upstream graph's guided AI review and cloud
read-aloud controls are not included in this local adapter.

Run the IntelliJ sandbox from the repository root:

```bash
./gradlew :plugin:idea:runIde
```

In the sandbox, choose **Tools > Open PaperConnekt**. Use **Open local PDF** in
the workspace menu to import a file, or open an attached Library paper. Graph
opens the actual interactive canvas; **Saved graphs** opens graph history.
`-Dpaperconnekt.data.dir` controls the shared data directory when supplied to the
IDE process; the default is `~/.paperconnekt-contest`.
