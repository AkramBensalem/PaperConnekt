# PaperConnekt

PaperConnekt is a local-first Kotlin Multiplatform research workspace built for the
[KotlinConf Student Contest](https://kotlinconf.com/contest/).

The contest edition focuses on a simple, private workflow: no account, no required
backend, and no Supabase dependency. Research data stays on the device. Optional
integrations, such as Zotero and AI providers, are configured locally by the user.

## Contest scope

- Onboarding for a first local workspace
- Research dashboard for discovering and saving papers
- AI assistance with conversation history and PDF opening
- Saved research graphs
- A local hierarchical library inspired by Zotero
- Secure local storage for optional provider keys
- Native Kotlin Multiplatform app and native plugin adapters

## Architecture

```text
shared/domain       Serializable research models and navigation contracts
shared/storage      Local repository interfaces and persistence implementations
shared/secrets      Secure-storage abstraction for provider keys
plugin/native-api   Shared contract for native IDE/editor integrations
app/composeApp      Compose Multiplatform Android and desktop application
```

The application has no `remote`, `remotedb`, `server`, or Supabase module. Features
depend on shared contracts rather than platform-specific UI or network services.

## Branch-by-branch development

The contest repository is developed in small feature branches:

1. `contest/foundation` - local-first modules, app shell, and project documentation
2. `contest/onboarding` - first-run setup and local workspace initialization
3. `contest/dashboard` - local paper storage with optional OpenAlex discovery
4. `contest/assistant` - local AI history, paper context, and PDF workspace
5. `contest/graph` - graph persistence, paper membership, and visualization
6. `contest/library` - hierarchical local library and paper organization
7. `contest/settings` - secure key storage and preferences
8. `contest/native-plugins` - native plugin adapters sharing the KMP contracts

Each branch should remain independently understandable and keep shared behavior in
`shared/` modules.

## Development

Use JDK 17 or newer and open the project in IntelliJ IDEA or Android Studio with
Kotlin Multiplatform support. The contest repository intentionally keeps platform
adapters small and puts feature logic in common source sets.

## License

See the repository license file for the applicable terms.
