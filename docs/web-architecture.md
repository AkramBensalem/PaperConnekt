# Web platform architecture proposal

## Decision summary

The web platform should be a **single local-first Compose Multiplatform Wasm
application** served by a small Ktor static host.

Ktor should own:

- serving the generated Wasm, JavaScript, resources, fonts, and icons;
- returning the web application's `index.html` for deep links such as
  `/library` or `/assistant`;
- optional development and packaging concerns such as compression, cache
  headers, and a health endpoint.

Ktor should **not** own:

- the research workspace;
- user sessions or authentication;
- paper, graph, assistant, or settings data;
- application navigation state;
- a required remote API or database.

The browser is the local client. Its storage adapters implement the same
`WorkspaceStorage`, `PdfAttachmentStorage`, and `SecretStore` contracts used by
desktop and Android. The web application therefore behaves like the desktop
application while using browser capabilities instead of a native filesystem.

## Why not one Wasm bundle per page?

Serving a separate Wasm executable for every page sounds like code splitting,
but it is not the best first architecture for this application.

Each Wasm executable would carry a Kotlin/Wasm and Compose runtime boundary. A
page transition would either reload the browser document or require several
independently initialized runtimes. That causes:

- repeated runtime and shared UI code in the download and cache footprint;
- slower transitions and loss of in-memory state;
- duplicated dependency injection and storage initialization;
- difficult coordination for PDF workspaces, assistant conversations, and graph
  navigation;
- a need for cross-page synchronization through `BroadcastChannel`,
  `SharedWorker`, or repeated persistence reads;
- more complicated browser history, back-button, and deep-link behavior.

The meaningful optimization is **route-level resource loading**, not a
separate Wasm runtime per page. The first web build should use one Wasm entry
point and allow the Kotlin/JS toolchain and browser cache to split or cache
JavaScript resources where supported. Heavy features can later be isolated
behind lazy feature boundaries after measuring the generated distribution.

If an independently deployable page ever becomes necessary, it should be a
separate application with an explicit persisted-state protocol—not the default
navigation model for the main application.

## Proposed runtime topology

```text
Browser tab
┌─────────────────────────────────────────────────────────────────────┐
│ app/web                                                             │
│  Wasm entrypoint                                                    │
│    └── WebBootstrap                                                  │
│         ├── BrowserWorkspaceStorage (IndexedDB)                     │
│         ├── BrowserPdfAttachmentStorage (IndexedDB / OPFS fallback)  │
│         ├── BrowserSecretStore (IndexedDB, origin-scoped)             │
│         ├── Browser capability adapters                             │
│         └── shared Compose application                              │
│              └── AppShell + existing feature screens                │
│                                                                     │
│ URL/history adapter  ⇄  AppDestination + route arguments            │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  │ static assets, optional health check
                                  ▼
                         Ktor static host
                                  │
                                  ▼
                    Local browser filesystem/storage
```

The Ktor process is optional for development and distribution. A static web
host can serve the same output in production, provided it supports the
fallback from application routes to `index.html`. No application data needs to
pass through Ktor.

## Module layout

The web platform should follow the existing contest boundaries:

```text
app/
├── composeApp/                 # shared Compose UI and application flow
│   └── src/commonMain/         # screens, AppShell, state, feature behavior
├── web/                        # new Wasm launcher and browser adapters
│   └── src/wasmJsMain/
│       ├── Main.kt             # ComposeViewport entrypoint
│       ├── WebBootstrap.kt     # browser dependency wiring
│       ├── WebWorkspaceStorage.kt
│       ├── WebPdfAttachmentStorage.kt
│       ├── WebSecretStore.kt
│       └── WebHistory.kt
└── webServer/                  # optional Ktor static host
    └── src/main/               # serves app/web distribution only
```

The exact `webServer` module name can be adjusted to match the reference
repository if a smaller server module is preferable. It must remain a host,
not a backend.

Shared Compose code should remain in `app/composeApp`. The web launcher should
only provide dependencies and platform behavior. Web-only code must not leak
browser APIs into common UI or domain models.

## Navigation and URL ownership

Navigation should be split by responsibility:

| Responsibility | Owner |
| --- | --- |
| Compose screen transitions and retained state | `AppShell` |
| Destination model and route arguments | shared navigation contract |
| Browser address bar and history entries | web URL adapter |
| Deep-link fallback to the application shell | Ktor/static host |
| Data persistence across reloads | browser storage adapters |

`AppShell` currently keeps `AppDestination` in Compose state. The web platform
should preserve that behavior and add a small adapter:

```text
URL /library
    └── web bootstrap parses route
         └── AppShell starts at AppDestination.LIBRARY

User selects Graph
    └── AppShell changes destination
         └── web adapter calls history.pushState("/graph")

Browser Back
    └── popstate
         └── web adapter emits AppDestination
              └── AppShell updates without reloading Wasm
```

Focused workspaces should use route arguments rather than separate bundles:

- `/assistant`
- `/assistant/chat`
- `/assistant/pdf/{paperId}`
- `/graph`
- `/graph/{graphId}`
- `/library`
- `/settings`

The route parser must validate identifiers and fall back to a safe destination
when a URL is malformed. The URL is a shareable view address, not the source of
truth for the workspace. The persisted local workspace remains authoritative.

## Browser-local persistence

The web implementation needs platform adapters because desktop file paths and
Android app-private files do not exist in a browser.

### Workspace data

Implement `WorkspaceStorage` with IndexedDB. Store one versioned workspace
snapshot initially, matching the existing `PersistentResearchStore` contract.
Use a named database and object store so future migrations can add separate
records without changing the common store API.

Writes should retain the existing guarantees:

- serialize before replacing the current snapshot;
- preserve unreadable data for recovery;
- report read and write failures explicitly;
- never silently turn a failed read into an empty workspace.

The browser adapter should use a temporary object-store record or equivalent
transactional replacement before publishing the new snapshot.

### PDF attachments

Store imported PDF bytes in IndexedDB first. Use the Origin Private File System
(OPFS) as an optimization when available, but keep IndexedDB as the supported
fallback. `Paper.pdfPath` should become an opaque managed attachment key on the
web; common code must not assume it is an operating-system path.

The browser cannot safely modify the user's source file. Imports must copy bytes
into managed origin storage just as desktop copies files into its attachment
directory.

### Secrets

Implement the existing `SecretStore` contract with an origin-scoped browser
storage strategy using IndexedDB. A later hardening pass can wrap values with
Web Crypto when a stable key-management policy is available.
Browser storage is not equivalent to Android Keystore or a desktop OS keychain:
the UI must communicate that browser secrets are protected only within the
origin and device profile. No secret should be included in the workspace
snapshot or URL.

### Multiple tabs

The first implementation should be single-tab correct. A later enhancement can
use `BroadcastChannel` to notify other tabs that the workspace changed and
ask them to reload the latest snapshot. It should not be required for ordinary
navigation.

## Network and provider behavior

The application remains local-first:

- local papers, graphs, notes, assistant history, settings, and imported PDFs
  work without Ktor or a network;
- optional OpenAlex, Zotero, and AI provider integrations continue to use
  explicit provider clients and user-supplied credentials;
- a provider outage must not prevent opening the local workspace;
- Ktor must not become a proxy that makes a remote service mandatory.

Where a browser capability differs from desktop, the web adapter should expose
an explicit unsupported result and keep the rest of the feature usable. For
example, native file pickers and desktop MCP hosting are platform features, not
reasons to introduce a backend.

## Performance plan

The first implementation should optimize the critical path rather than
prematurely creating page bundles:

1. Keep the web launcher thin and reuse `app/composeApp`.
2. Keep browser storage initialization lazy but complete before showing the
   workspace.
3. Cache immutable Wasm, JavaScript, fonts, and resource files with content
   hashes when the distribution supports them.
4. Compress static assets in Ktor and set long-lived cache headers for hashed
   files.
5. Avoid loading PDF bytes, graph data, or assistant history until the feature
   needs them.
6. Measure the production Wasm distribution before introducing feature-level
   lazy loading.

A service worker can be added later for offline asset caching. It must be
treated separately from application data persistence: cached assets make the
shell available offline, while IndexedDB/OPFS holds the user's workspace.

## Implementation sequence

This branch should initially document and then implement the web platform in
small slices:

1. Add `app/web` with a Wasm browser entrypoint and a minimal HTML shell.
2. Add browser implementations for workspace storage and secrets, with
   explicit failure handling.
3. Add URL/history integration around the existing `AppDestination` model.
4. Reuse the full shared `PaperConnektApp` with web capability flags.
5. Add an optional Ktor static host that serves the distribution and deep-link
   fallback.
6. Add web-specific PDF import/attachment support and feature capability
   handling.
7. Measure the production distribution and only then consider lazy resource
   loading or a service worker.

This keeps the web branch local-first and maximizes Compose reuse while
avoiding a server-backed architecture that the contest application explicitly
does not require.
