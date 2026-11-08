# DevOps and GitHub Actions

This project uses GitHub Actions to build PaperConnekt on GitHub-hosted
machines. The workflow is defined in
[`.github/workflows/ci.yml`](../.github/workflows/ci.yml). It does not need
provider credentials, a database, or a deployment server.

## What happens automatically

The workflow runs for:

- pull requests;
- pushes to `main` and `contest/*` branches; and
- a manual run started from the **Actions** tab with **Run workflow**.

It currently has three independent jobs:

| Job | Purpose | Output |
| --- | --- | --- |
| Gradle | Runs `check`, then builds the desktop JVM, Wasm web, and Ktor server distributions | `paperconnekt-jvm-web` |
| Android | Builds the debug Android application with API 37 | `paperconnekt-android-debug` |
| VS Code | Installs extension tooling and packages the extension | `paperconnekt-vscode` |

When a run succeeds, open the run in GitHub and download its artifacts from the
**Artifacts** section. These are build outputs for review and testing, not
signed public releases.

## Beginner workflow

1. Create a branch and make a focused change.
2. Push the branch to GitHub and open a pull request.
3. Open the **Actions** tab and select **Build PaperConnekt**.
4. Read the failed step if a job is red. The step name and command identify
   whether the problem is Gradle, Android SDK setup, or the VS Code package.
5. Fix the issue locally, push again, and let GitHub rerun the workflow.
6. Download an artifact when you need to inspect the remotely built output.

The workflow uses the Gradle wrapper committed to this repository. This keeps
the Gradle version consistent between a developer machine and GitHub. Gradle
and dependency caches are managed by `gradle/actions/setup-gradle`.

## Why the jobs are split

Android, JVM/Wasm, and Node.js have different toolchains. Separate jobs make
failures easier to understand and allow GitHub to run them in parallel. The
Linux runner can build the desktop and web targets, but it cannot validate
Apple packaging; that will require a macOS runner and Apple signing setup.

## Roadmap

The safe order for extending automation is:

1. **Continuous integration (implemented):** build and package every change.
2. **Quality gates:** add focused tests, formatting, dependency review, and
   required branch checks after the baseline workflow is stable.
3. **Release artifacts:** create a tag-triggered workflow for versioned,
   signed Android and desktop packages. Signing keys must be stored as
   encrypted GitHub Actions secrets, never in the repository.
4. **Deployment:** publish the web container or static bundle from a protected
   environment only after a release is approved. Deployment credentials should
   use environment secrets and least-privilege permissions.

No deployment or signing secrets are needed for the current workflow. Do not
add a secret just to make a local provider integration work; PaperConnekt keeps
those integrations optional and local-first.

## Troubleshooting

- **A workflow does not start:** confirm the branch matches one of the push
  patterns, or use **Run workflow**.
- **Gradle fails before compilation:** check the JDK and Gradle wrapper steps,
  then reproduce with the same `./gradlew` command locally.
- **Android fails during SDK setup:** confirm the project still uses the API
  level installed by the workflow.
- **An artifact is missing:** inspect the producing job first; the upload step
  intentionally fails when its expected output was not generated.
