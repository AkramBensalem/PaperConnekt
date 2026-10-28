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

