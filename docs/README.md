# PaperConnekt documentation

This directory contains the project documentation that is too detailed for the
root README. Start with the guide that matches what you are trying to do:

| I want to... | Read |
| --- | --- |
| Install and run PaperConnekt | [Installation](installation.md) |
| Understand the module boundaries | [Architecture](architecture.md) |
| Work on the codebase | [Development](development.md) |
| Understand the web target | [Web architecture](web-architecture.md) |
| Understand paper retrieval and AI tools | [AI agent architecture](ai-agent-architecture.md) |
| Understand remote builds and GitHub Actions | [DevOps and GitHub Actions](devops.md) |
| See the planned feature slices | [Feature roadmap](branch-plan.md) |
| Run or extend the native plugins | [Native plugin notes](../plugin/native-plugins/README.md) |

## Documentation map

### Getting started

- [Installation](installation.md) covers prerequisites, cloning, Docker, and
  platform-specific launch commands.
- [Development](development.md) covers the repository layout, useful tasks, and
  contribution rules.

### Design and architecture

- [Architecture](architecture.md) is the source of truth for shared contracts,
  local-first persistence, PDF attachments, and feature boundaries.
- [Web architecture](web-architecture.md) documents the browser storage and
  Wasm hosting approach.
- [AI agent architecture](ai-agent-architecture.md) documents retrieval,
  citations, embeddings, and MCP.
- [DevOps and GitHub Actions](devops.md) explains the remote build workflow,
  downloadable artifacts, and the path toward releases and deployment.

### Planning and integrations

- [Feature roadmap](branch-plan.md) lists the contest feature slices.
- [Native plugin notes](../plugin/native-plugins/README.md) documents the
  IntelliJ IDEA and VS Code integrations.

When adding a new technical document, link it here and from the relevant
section of the root README. Keep this page short: it is a directory, not a
second README.
