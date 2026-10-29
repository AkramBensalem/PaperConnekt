# Contest architecture

## Principles

1. **Local first:** the app works without an account, server, or network connection.
2. **Shared contracts:** domain models, storage contracts, and feature state live in
   Kotlin Multiplatform modules.
3. **Native integrations:** Android, desktop, Zotero, AI providers, and editor
   plugins are optional adapters around shared contracts.
4. **Small feature branches:** every user-visible capability is introduced in a
   focused branch with its own documentation.
5. **Secure secrets:** provider keys are accessed through `SecretStore`; feature code
   never writes plaintext secrets to ordinary app preferences.

## Dependency direction

```text
app/android ───────┐
app/desktop ───────┼──> app/composeApp ──> shared/storage ──> shared/domain
future app/web ────┘             │                  └───────> shared/secrets
                                 └──────────────> plugin/native-api
```

`app/composeApp` contains the shared Compose UI and application flow. Platform
modules own their launchers, manifests, window configuration, and native
implementations such as secure secret storage. This keeps Android and desktop
independent while allowing a future web module to consume the same shared UI
and contracts.

`shared/domain` has no UI or network dependency. `shared/storage` owns local
repositories and can later add platform persistence without changing feature APIs.
`shared/secrets` exposes only a secure-storage contract. Native plugin modules may
consume the same domain models but must not introduce Supabase or remote session
requirements.

The settings surface keeps workspace preferences in `AppSettings` and provider
credentials behind `SecretStore`. Compose only tracks whether a credential is
configured and never repopulates a saved key into the UI; Android and desktop
continue to own their respective secret-storage boundaries. OpenAlex discovery
and Zotero remain explicitly optional settings, so local collection and assistant
history remain usable without network access or an account.

The contest workspace is persisted as a versioned JSON snapshot by
`PersistentResearchStore`. Desktop stores it at
`~/.paperconnekt/workspace.json`; Android stores it in the app-private
`files/paperconnekt/workspace.json` directory. Writes go through a temporary file
and replacement, and a malformed snapshot is moved to `workspace.json.corrupt`
before the store starts with empty defaults. The snapshot contains papers,
assistant sessions and messages, graphs, library entries, and `AppSettings`; it
never contains provider secrets, which remain owned by `SecretStore`. Schema
changes must increment the snapshot version and add an explicit migration before
the current model is accepted; unsupported versions are preserved as corrupt
backups rather than silently discarded.

## Local PDF attachments

Library item details provide a PDF drop target and a native file picker, with
progress, validation feedback, and confirmation before replacing an attachment.
The same details are available in a dialog on compact layouts. Use **View** (or
**Attach PDF** in the item menu), drop one PDF or choose **Browse PDF**, then
choose **Open PDF** to enter **Assistant / PDF Viewer**.

`PdfAttachmentStorage` is the shared import/read contract. Its desktop and Android
adapters validate the PDF header and copy the document into a unique managed
folder: `~/.paperconnekt/attachments` on desktop and the app-private
`files/paperconnekt/attachments` directory on Android. `Paper.pdfPath` stores the
managed path in the existing workspace snapshot; source files are never modified,
and moving the original does not break the attachment. Existing manually entered
paths remain readable. Attachments stay local and require no provider or sign-in.
Replacement does not delete the previous managed copy.

Native picker and drag-and-drop integrations live in `app/desktop` and
`app/android`, injected into shared Compose UI through `PdfAttachmentUi`.
The Assistant workspace reuses `PdfWorkspaceViewModel.openLocal` and the existing
PDF reader for actual rendering, page navigation, zoom, and text selection.
The local PDF screen hosts the reference Assistant's `Workplace` directly, using
its single reader header and the same Notes, AI Agent, and Ideas tabs,
`NotesManager`, `PaperAgentPanel`, and `IdeaOrganizer` components. Workspace-scoped
ViewModels are cleared on exit. Local paper IDs namespace saved idea notes;
attachment-specific keys isolate agent chat history and OCR Markdown when a PDF
is replaced, without requiring an account. Ideas use
the existing local Markdown store; research notes retain the reference feature's
session-only behavior. Local PDFs do not enable Zotero annotation writes.
The reference sidebar is reused, not a second implementation. This does not
restore the startup app's provider/backend services: the contest research-agent
adapter explicitly reports unavailable provider execution instead of returning a
placeholder response, and server-generated read-aloud audio is not wired.

The platform launchers explicitly provide `Dispatchers.Main` for the reader's
`viewModelScope`: desktop uses `kotlinx-coroutines-swing` (the AWT event dispatch
thread), and Android uses `kotlinx-coroutines-android`. Both use the same catalog
version as `kotlinx-coroutines-core`.

## Contest feature slices

Each feature should follow the same shape:

```text
feature/<name>/
├── api/          navigation and public contracts
├── model/        feature-specific state
├── data/         local repository use cases
└── ui/           Compose screens and components
```

The initial foundation keeps the app shell intentionally small. Later branches
should extract each placeholder screen into a feature module only when its shared
contract is stable.
