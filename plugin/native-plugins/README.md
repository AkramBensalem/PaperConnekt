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

This keeps platform integration code native while allowing the app and plugins to
exchange the same Kotlin data models and plugin contract. No JavaScript extension
runtime or remote service is required by these modules.

## Brand resources

Both adapters carry exports of `branding/paperconnekt.svg`. Regenerate them
with `python3 tools/generate_brand_icons.py` from the repository root.

| Host surface | Resource |
| --- | --- |
| IntelliJ plugin listing | `idea/src/main/resources/META-INF/pluginIcon.svg` and `pluginIcon_dark.svg` (40 px) |
| IntelliJ tool window/action | `idea/src/main/resources/icons/paperconnekt.svg` and `paperconnekt_dark.svg` (16 px, theme-specific blue) |
| VS Code extension listing | `vscode/src/main/resources/icons/paperconnekt.png` (128 px) |
| VS Code activity bar | `vscode/src/main/resources/icons/paperconnekt-activity.svg` (24 px, monochrome for host tinting) |

These are Kotlin adapter modules, not yet packaged IntelliJ or VS Code
extensions. IntelliJ's listing icons use the conventional `META-INF` names;
a future tool-window registration should reference `/icons/paperconnekt.svg`.
When adding the VS Code extension manifest, copy the assets into the extension
package and set its `icon` and `contributes.viewsContainers.activitybar[].icon`
paths to the listing and activity-bar resources respectively. No inert
`plugin.xml` or `package.json` is added merely to declare an icon.
