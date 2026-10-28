# Contest branch plan

All branches are based on the previous contest branch and are pushed to the
`PaperConnekt` repository. The first two branches establish the foundation and
first-run experience before feature work begins.

| Branch | Deliverable |
| --- | --- |
| `contest/foundation` | KMP modules, local-first contracts, app shell, docs |
| `contest/onboarding` | First-run onboarding and workspace initialization |
| `contest/dashboard` | Local paper search, paper cards, optional OpenAlex discovery, and local saves |
| `contest/assistant` | Local assistant history, paper context, and PDF workspace contract |
| `contest/graph` | Saved graph model, graph list, and graph canvas |
| `contest/library` | Folder hierarchy, paper moves, and local import |
| `contest/settings` | Secure provider keys, theme, and optional Zotero settings |
| `contest/native-plugins` | Native IDE/editor adapters using shared KMP contracts |

No branch may add Supabase, mandatory authentication, or a required server.
