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
They are intentionally read-only for now: edits remain owned by the main app,
which prevents a plugin refresh from overwriting newer local data.
