# Installation and running

PaperConnekt is local-first: no account, backend, or provider key is required
to use the core workspace.

## Choose the kind of installation

| Your goal | Follow |
| --- | --- |
| **Use PaperConnekt without setting up a development environment** | [Run the web app with Docker](#option-a-run-the-web-app-with-docker) |
| **Run the desktop app on Windows or Linux from this repository** | [Build and run from source](#option-b-build-and-run-from-source) |
| **Develop PaperConnekt or package an installer** | [Developer setup](#developer-setup-and-builds) |

There is not yet a published `.exe`, `.msi`, or Linux package. Until a release
package is published, the desktop app must be built from source. Docker is the
easiest way to try the web app on either Windows or Linux.

## Option A: run the web app with Docker

This option does not require Java, Gradle, or an IDE. Install Docker Desktop on
Windows, or Docker Engine and the Compose plugin on Linux, then run:

```bash
git clone https://github.com/AkramBensalem/PaperConnekt.git
cd PaperConnekt
docker compose up --build
```

Open <http://localhost:8080>. Keep the terminal open while using the app. Stop
the app with `Ctrl+C`, or from another terminal run:

```bash
docker compose down
```

The web target stores workspace data in the browser. The container is only a
local static asset host; it is not a database or remote service.

## Option B: build and run from source

Use this option for the native desktop application. It works on supported
64-bit Windows and Linux systems where JDK 17 or newer is available.

### 1. Install the prerequisites

- **Windows:** install [JDK 17 or newer](https://adoptium.net/temurin/releases/)
  and [Git](https://git-scm.com/download/win).
- **Linux:** install a JDK 17 or newer and Git using your distribution's
  package manager. For Ubuntu/Debian, for example:

  ```bash
  sudo apt update
  sudo apt install openjdk-17-jdk git
  ```

  For Fedora:

  ```bash
  sudo dnf install java-17-openjdk-devel git
  ```

An IDE is not required. The repository includes the Gradle wrapper, so Gradle
is downloaded automatically.

### 2. Download the source

```bash
git clone https://github.com/AkramBensalem/PaperConnekt.git
cd PaperConnekt
```

### 3. Start the desktop app

On Linux, open a terminal and run:

```bash
./gradlew :app:desktop:run
```

On Windows PowerShell, run:

```powershell
.\gradlew.bat :app:desktop:run
```

The first run can take several minutes while Gradle downloads dependencies.
PaperConnekt stores desktop workspace data in
`~/.paperconnekt-contest` by default. To use another directory, add
`-Dpaperconnekt.data.dir=/path/to/workspace` on Linux, or
`-Dpaperconnekt.data.dir=C:\path\to\workspace` on Windows.

## Developer setup and builds

Install the prerequisites above, then open the repository root in Android
Studio or IntelliJ IDEA with Kotlin Multiplatform support. Allow Gradle to
import the project. Do not commit `local.properties`, IDE files, build output,
provider keys, or local workspace data.

The regular desktop and web server modules use JDK 17. The IntelliJ plugin is
built against the IntelliJ 2026.2 platform and uses a JDK 21 toolchain for that
module.

To create a native desktop distribution instead of launching immediately, run
the following from the repository root.

On Linux:

```bash
./gradlew :app:desktop:packageDistributionForCurrentOS
```

On Windows PowerShell:

```powershell
.\gradlew.bat :app:desktop:packageDistributionForCurrentOS
```

The generated package is written under
`app/desktop/build/compose/binaries/main`. This is a locally built package, not
an officially signed release.

## Run another target

### Desktop

```bash
./gradlew :app:desktop:run
```

Desktop workspace data is stored in `~/.paperconnekt-contest` by default. To
use another directory, pass `-Dpaperconnekt.data.dir=/path/to/workspace` to
the JVM process.

### Android

Start an emulator or connect a device, then run:

```bash
./gradlew :app:android:installDebug
```

You can also select the Android run configuration in Android Studio. Android
stores data inside the app-private sandbox and uses the system document picker
for PDF imports.

### Web

For the easiest web setup, use [Option A](#option-a-run-the-web-app-with-docker).
To build and serve the web target manually, use the developer commands in
[Development](development.md).

### IntelliJ IDEA plugin

```bash
./gradlew :plugin:idea:runIde
```

In the sandbox, choose **Tools > Open PaperConnekt**. The plugin uses the same
local workspace format as the desktop app. See the
[native plugin notes](../plugin/native-plugins/README.md) for plugin-specific
behavior and limitations.

### VS Code extension

The extension is in `plugin/vscode`. Install Node.js and npm, then package it
with:

On Linux:

```bash
cd plugin/vscode
npm install
npm run package
```

On Windows PowerShell, use the same commands after changing to the directory:

```powershell
cd plugin/vscode
npm install
npm run package
```

Install the generated `.vsix` from VS Code's **Extensions** view. The
extension reads the shared workspace and is intentionally read-only.

## Optional providers

OpenAlex, Zotero, and AI providers are optional. Configure credentials from
the application's settings when you need those integrations. Credentials are
handled through the platform secret boundary and are not part of
`workspace.json`.

## Troubleshooting

- **Gradle cannot find Java:** set `JAVA_HOME` to a JDK 17+ installation.
- **Windows says scripts are disabled:** use `.\gradlew.bat`, not
  `./gradlew`, in PowerShell.
- **Linux says `gradlew` is not executable:** run `chmod +x gradlew` once, then
  retry `./gradlew :app:desktop:run`.
- **Android install fails:** check that an emulator/device is connected and
  that SDK 35 is installed.
- **Web page does not load:** make sure Docker is running and port `8080` is
  free, then retry `docker compose up --build`.
- **A workspace is not visible:** confirm the process is using the expected
  `paperconnekt.data.dir`; the default is `~/.paperconnekt-contest`.

For architecture and storage details, see
[Architecture](architecture.md).
