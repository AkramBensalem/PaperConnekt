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

The Dashboard, Library, Assistant, Graph list, and Settings share
`WorkspaceHeader`: a responsive title surface with Material icons, summary chips,
and wrapping actions. Library and graph search use the same labelled field with
clear-search controls. Primary actions retain the brand blue; backgrounds, cards,
borders, text, and secondary actions use coordinated cool blue/slate tones, with
muted violet tertiary accents in both light and dark themes. Header entrance, button hover/press,
and search focus use finite Compose animations (no continuous decorative motion).
Focused PDF and graph-canvas workspaces retain their task-specific toolbars and
occupy the full page without the application navigation rail or bottom bar.
On compact Library layouts, **Browse collections** opens the collection picker
in a dismissible dialog so the header and folder tree do not crowd out papers.
View filters and header actions wrap rather than truncating on narrow windows.
When Zotero is configured, the Library header can switch between the local
collection and a read-only Zotero browser; all top-level Zotero items are
loaded, whether or not they have a PDF attachment. Zotero collections and PDF
availability are loaded on demand without changing local Library data. The
Zotero browser keeps the same nested collection hierarchy as the local Library,
places the compact personal/group switcher in the Collections header, and lets
researchers open available PDFs in the shared reader. Zotero reader annotations
use the existing version-safe synchronization path so edits remain in Zotero.
The selected Library source, group, collection, folder, expansion state, filter,
and search query are saved in `AppSettings`, so leaving and reopening the Library
restores the previous browsing context.

The shared theme supplies blue interaction state layers rather than inheriting
gray text colors for hover and ripple feedback. Hover is subtle; keyboard focus,
press, and drag feedback remain visible. Clickable cards and rows use Material's
clickable surfaces so indications share their rounded bounds. Graph chips reuse
the shared rounded interaction helper instead of a no-op compatibility shim.
Foundation clickables explicitly inherit the blue Material indication as well.
Library view filters use `WorkspaceFilterChip`: a selectable rounded row with
`indication = null` and explicit animated blue hover/press containers. A stronger
border marks keyboard focus; tab selection semantics and keyboard activation
remain intact. No default ripple or hover overlay is used for these filters.

All Material typography roles use bundled **DM Sans** from Google Fonts, with
regular, medium, semibold, and bold TrueType weights in shared Compose resources.
Headings use tighter tracking; body text uses more generous line spacing for
reading. Fonts work offline on Android and desktop without fetching a web font
at runtime. The SIL Open Font License is bundled as
`composeResources/files/dm_sans_license.txt`. Explicit monospace code and serif
math/editor styles keep their existing specialized font families.

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
OCR and paper-agent features read their provider keys from that same secret
boundary. Missing keys block the corresponding request; rejected keys and
network failures are shown as actionable provider errors, while local reading,
notes, and saved research remain available offline. A saved key is not treated
as validated until its provider accepts a request.

The contest workspace is persisted as a versioned JSON snapshot by
`PersistentResearchStore`. Desktop stores it at
`~/.paperconnekt-contest/workspace.json`; Android stores it in app-private,
non-backed-up `noBackupFilesDir/paperconnekt-contest/workspace.json`. All desktop
research stores use the same root (overridable with `paperconnekt.data.dir`),
separate from the previous application's `~/.paperconnekt` directory. Platform
storage instances are remembered for the application lifetime, not recreated
during recomposition. Writes go through a temporary file
and replacement, and a malformed snapshot is moved to `workspace.json.corrupt`
before the user can explicitly retry with empty defaults. Read failures are
shown instead of silently appearing as an empty Library. The snapshot contains papers,
assistant sessions and messages, graphs, library entries, `AppSettings`, and
per-document PDF reading positions; it never contains provider secrets, which
remain owned by `SecretStore`. Local and Zotero PDF positions use stable item or
attachment keys and save the page, scroll fraction, and zoom, so reopening a
document—even after restarting the app—returns to the last saved location.
Incompatible schema changes must increment the snapshot version and add an explicit
migration; additive fields with defaults remain backward compatible. Unsupported
versions are preserved as corrupt backups rather than silently discarded.

The Library and Assistant paper shelf contain only explicitly saved items:
manual additions, **Save to Library**, or **Keep** in guided review. Graph seed
selection and automatic reference/citation expansion do not save Library items.
Graph discovery metadata is stored in `ResearchGraph.discoveredPapers` alongside
the graph's connections so reopening a graph does not require polluting the
Library. A saved item's metadata takes precedence over graph discovery metadata.
Discovery never writes to the saved-paper store. Item updates transform the
latest item under the store mutex; metadata upserts cannot clear an attached PDF.
Workspace mutations are written successfully before publishing updated state,
and in-flight writes finish even if the initiating screen is disposed.

The Library owns paper acquisition through **Add paper > Find on OpenAlex** and
**Add paper > Add manually**, on both compact and wide layouts. OpenAlex lookup
reuses the graph seed picker's result cards and pagination, searching by title,
author, keywords, or DOI only when requested. Results are not saved until
**Save locally** is selected; existing Library items show **Saved** and are not
overwritten. New items appear in **All papers** and can be moved into a collection.
Manual entry accepts metadata and an optional managed PDF copy. Save failures
keep the dialog open with the entered values and an error for retry.
The manual-entry dialog uses a two-column metadata/attachment layout on wide
windows and a scrollable single column on compact windows. Its header and action
footer remain visible while scrolling. The title receives initial keyboard
focus; optional year input is validated inline. Saving and PDF import disable
conflicting actions, and save errors appear beside the fixed footer.

On POSIX desktops the app creates data directories with mode `0700` and files
with mode `0600`; Android uses its application sandbox and disables backup.
These are local access controls, not encryption or protection against the OS
administrator or other processes running as the same desktop user. The app
never needs to modify the original PDF selected by the user.

## Local PDF attachments

Library item details provide a PDF drop target and a native file picker, with
progress, validation feedback, and confirmation before replacing an attachment.
The same details are available in a dialog on compact layouts. Use **View** (or
**Attach PDF** in the item menu), drop one PDF or choose **Browse PDF**, then
choose **Open PDF** to enter **Assistant / PDF Viewer**.

`PdfAttachmentStorage` is the shared import/read contract. Its desktop and Android
adapters validate the PDF header and copy the document into a unique managed
folder: `~/.paperconnekt-contest/attachments` on desktop and the app-private
`noBackupFilesDir/paperconnekt-contest/attachments` directory on Android. `Paper.pdfPath` stores the
managed path in the existing workspace snapshot; source files are never modified,
and moving the original does not break the attachment. Manual additions use the
same copy/import control, not a raw external file path; the reader accepts only
managed attachments. Attachments stay local and require no provider or sign-in.
Replacement does not delete the previous managed copy.

Native picker and drag-and-drop integrations live in `app/desktop` and
`app/android`, injected into shared Compose UI through `PdfAttachmentUi`.
The Assistant workspace reuses `PdfWorkspaceViewModel.openLocal` and the existing
PDF reader for actual rendering, page navigation, zoom, and text selection.
The local PDF screen hosts the reference Assistant's `Workplace` directly, using
its single reader header and the same Annotations, AI Agent, and Ideas tabs,
`AnnotationList`, `PaperAgentPanel`, and `IdeaOrganizer` components. Workspace-scoped
ViewModels are cleared on exit. Local paper IDs namespace saved idea notes;
attachment-specific keys isolate agent chat history and OCR Markdown when a PDF
is replaced, without requiring an account. Ideas use the existing local Markdown
store, including saved card positions and colors. Agent chat history must load
successfully before editing is enabled, and load/save errors expose retry rather
than overwriting unreadable history with an empty list. In-flight board and chat
saves survive normal workspace disposal. Local PDFs support the same page notes, page ideas, pen strokes,
rectangles, text boxes, highlights, underlines, editing, movement, resizing,
recoloring, and deletion as the Zotero reader. Scanned pages still need a text
layer for text selection, highlights, and underlines; drawing, text boxes, and
page notes work without one.

Local annotations reuse the existing annotation payload and rendering code.
`StoredLocalPdfAnnotations` serializes versioned snapshots through the shared
`PdfAttachmentStorage` contract. Android and desktop atomically replace a
per-attachment JSON file in `attachments/annotations`, keyed by a SHA-256 digest
of the attachment storage key. They do not modify the source PDF or call Zotero.
Saved annotations reload on reopening; unreadable snapshots block editing rather
than being silently overwritten. Failed saves retain the visible pending changes
and expose retry controls. The reader shows **Save locally** and local save
status instead of claiming Zotero synchronization. Reader exit and application
navigation warn before discarding a draft or pending local changes.
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
