# Building DreamJEmu from source

This covers cloning the repository and building it locally. See [`docs/MINIMUM_REQUIREMENTS.md`](MINIMUM_REQUIREMENTS.md) for end-user hardware/OS requirements — this document is about *building*, which has its own (slightly different) prerequisites.

## Prerequisites

- **Git**, to clone the repository.
- **A JDK 21 (or newer) with `jpackage`** — a full JDK, not a JRE, and not a "headless"-only package missing `jmods` (needed if you want to build the native app-image; the rest of the build works with a lighter JDK too). On Debian/Ubuntu: `sudo apt install openjdk-21-jdk`.
- You do **not** need Gradle installed separately — the repository includes the Gradle wrapper (`gradlew` / `gradlew.bat`), which downloads the correct Gradle version automatically on first use.
- An internet connection for the first build — Gradle needs to download the Gradle distribution itself plus this project's dependencies (JavaFX, LWJGL, JUnit — see [`docs/DEPENDENCIES.md`](DEPENDENCIES.md)) from their usual public repositories.

## Clone the repository

```bash
git clone https://github.com/Gih-pt/dreamjemu.git
cd dreamjemu
```

(Use the SSH URL, `git@github.com:Gih-pt/dreamjemu.git`, instead if you have SSH keys set up with GitHub — see GitHub's own docs on SSH keys if you want to avoid re-entering credentials on every push, for anyone with write access.)

## Point Gradle at your JDK

Gradle needs to know which JDK to use. The simplest way is to set `JAVA_HOME` for the command:

```bash
# Linux example — adjust the path to wherever your JDK 21 is installed
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew build
```

On Windows (PowerShell):
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
.\gradlew.bat build
```

On macOS, `JAVA_HOME` is usually already set correctly if you installed a JDK via Homebrew or the official installer; if not, use `/usr/libexec/java_home -v 21` to find the path.

If you don't want to prefix every command with `JAVA_HOME=...`, set it once for your shell session (`export JAVA_HOME=...` on Linux/macOS) or system-wide via your OS's environment variable settings.

## Building on a low-memory machine

The repository's committed [`gradle.properties`](../gradle.properties) already uses conservative settings (`-Xmx768m`, no persistent daemon, no parallel builds) so the project builds reliably even on modest hardware (~4GB RAM). If you're on a more capable machine and want faster builds, you can override these *locally* (don't commit personal overrides) via `~/.gradle/gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m
org.gradle.daemon=true
org.gradle.parallel=true
```

## Build everything and run the tests

```bash
./gradlew build --no-daemon
```

This compiles every module and runs all unit/integration tests (`core-system`, `core-gdrom`, `core-cpu-sh4`, etc.). `--no-daemon` matches the repository's conservative-memory defaults; drop it if you've enabled the daemon per above.

To run a single module's tests only:
```bash
./gradlew :core-cpu-sh4:test --no-daemon
```

## Run the JavaFX app directly (fastest way to try it)

```bash
./gradlew :app-javafx:run --no-daemon
```

This compiles (if needed) and launches the app straight from Gradle — no packaging step, good for quick iteration.

## Build a native, standalone app image

```bash
./gradlew :app-javafx:jpackageImage --no-daemon
```

This produces a self-contained folder with a bundled Java runtime and a native launcher — no separate Java install needed to run it. See [`docs/STATUS.md`](STATUS.md) for exactly what this does and doesn't do yet (it's an unsigned app-image, not a signed installer).

Output location and how to run it:

| Platform | Path |
|---|---|
| Linux | `app-javafx/build/jpackage/DreamJEmu/bin/DreamJEmu` |
| Windows | `app-javafx\build\jpackage\DreamJEmu\DreamJEmu.exe` |
| macOS | `app-javafx/build/jpackage/DreamJEmu.app` |

Run it directly — no `gradlew`, no `JAVA_HOME`, no separate Java installation required:
```bash
app-javafx/build/jpackage/DreamJEmu/bin/DreamJEmu   # Linux example
```

Note: this is *not* the Linux `.AppImage` single-file format, despite the similar name — "app-image" here is jpackage's generic term for "self-contained folder." Producing an actual portable `.AppImage` file, or signed installers (.msi/.dmg/.deb), is a possible future improvement — see [`docs/ROADMAP.md`](ROADMAP.md).

## Troubleshooting

- **"JavaFX runtime components are missing"**: shouldn't happen anymore — the app's actual entry point is `Launcher`, not `Main`, specifically to avoid this (see `Launcher`'s Javadoc in `app-javafx/src/main/java/org/dreamjemu/app/Launcher.java`). If you hit it anyway, double-check `application.mainClass` in `app-javafx/build.gradle.kts` still points at `Launcher`.
- **Build seems to hang or the machine becomes unresponsive**: you likely need the low-memory `gradle.properties` settings described above (they're committed by default, so this should only happen if you've overridden them locally with something too aggressive for your hardware).
- **`jpackage` errors about missing `jmods`**: you have a JRE or a stripped-down JDK package without `jmods`. Install a full JDK (e.g. `openjdk-21-jdk`, not `openjdk-21-jre` or `openjdk-21-jdk-headless` on some distros — check with `ls $JAVA_HOME/jmods`).
- **Git push authentication issues**: see GitHub's documentation on [personal access tokens](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens) (make sure the token has both `repo` and `workflow` scopes if you're pushing changes to `.github/workflows/`) or [SSH keys](https://docs.github.com/en/authentication/connecting-to-github-with-ssh) for a more permanent fix.

## Next steps

Once you can build and run the project, see [`CONTRIBUTING.md`](../CONTRIBUTING.md) for how to contribute changes, and [`docs/STATUS.md`](STATUS.md) / [`docs/ROADMAP.md`](ROADMAP.md) for what's implemented and what to work on next.
