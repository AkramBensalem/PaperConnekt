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
