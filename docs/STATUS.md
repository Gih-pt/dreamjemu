# Project Status

*Last updated: 2026-07-22 (jpackage native app-image packaging added; GDI reading implemented; CI nightly/release workflow YAML fixed). Update this file whenever a contribution meaningfully changes what's implemented — see `CONTRIBUTING.md`.*

## Current state: bootstrap complete; system bus, disc reading, and native packaging implemented; CI fully green

The project scaffold is in place and verified end-to-end. Real emulation infrastructure now exists and is tested: the system memory bus, Dreamcast disc image format detection, and GDI parsing/sector reading. The app can also now be packaged into a native, self-contained executable (no separate Java install needed for end users). All three GitHub Actions workflows (Build, Nightly, Release) are syntactically valid; Build passes on every push across Windows/macOS/Linux.

### Done so far

- [x] Project charter, principles, repository structure, contribution rules, AI guidance, issue/PR templates.
- [x] Repository published publicly on GitHub (github.com/Gih-pt/dreamjemu).
- [x] Gradle wrapper committed; multi-module build verified successful (Linux, Java 21, Gradle 8.7).
- [x] Conservative `gradle.properties` committed for low-RAM machines.
- [x] Placeholder JavaFX window (`app-javafx`) verified to launch and display correctly.
- [x] CI verified on GitHub Actions: `build.yml` passes on `windows-latest`/`macos-latest`/`ubuntu-latest`. `nightly.yml`/`release.yml` invalid-YAML bug fixed and confirmed working.
- [x] `core-system`: memory map and system bus implemented and tested (7 JUnit tests passing).
- [x] `core-gdrom`: disc image format detection implemented and tested (`DiscImageDetector` — GDI, CDI, CHD, CUE/BIN; 8 JUnit tests passing).
- [x] `core-gdrom`: GDI parsing and sector reading implemented and tested (`GdiImage`/`GdiTrack`/`GdiTrackType`; 7 JUnit tests passing). 15 tests total in `core-gdrom`.
- [x] `docs/DEPENDENCIES.md` added: every third-party dependency, its purpose, and GPLv3 license-compatibility check.
- [x] `CONTRIBUTING.md` now requires: mandatory AI-usage disclosure (yes/no) on every PR, keeping `docs/STATUS.md` / `docs/ROADMAP.md` / `CHANGELOG.md` current (with dates) on every impactful PR, and updating `docs/DEPENDENCIES.md` when dependencies change.
- [x] **`app-javafx`: native app-image packaging via `jpackage`.**
  - New Gradle task `:app-javafx:jpackageImage` builds a self-contained application image with a bundled Java runtime and a platform-native launcher (`DreamJEmu.exe` / `DreamJEmu.app` / `DreamJEmu`) — end users won't need Java installed separately.
  - Currently produces an app-image (a runnable folder), **not yet a signed installer** (.msi/.dmg/.deb) — that needs additional platform-specific tooling (WiX on Windows, codesigning certs on macOS, dpkg-dev on Linux) and is a follow-up step, not yet implemented.
  - Wired into `.github/workflows/nightly.yml` and `.github/workflows/release.yml`, replacing their previous `TODO: jpackage...` placeholder steps; both now upload the packaged app-image as a build artifact.
  - `jpackage` itself was smoke-tested end-to-end in a sandbox (trivial jar → app-image → ran the generated native launcher successfully), and the updated workflow YAML was validated with a real YAML parser. **The actual `:app-javafx:jpackageImage` Gradle task has not yet been run against the full project on a real machine — this needs local confirmation.**

### Not started yet

- [ ] SH-4 CPU core (interpreter) — stub class exists (`core-cpu-sh4`), can now be wired to the real `Bus` implementation from `core-system`.
- [ ] PowerVR2 GPU core.
- [ ] AICA sound core.
- [ ] Maple bus (controllers, VMU).
- [ ] CDI/CHD/CUE-BIN *reading* (only GDI reading exists so far; detection exists for all four formats).
- [ ] BIOS-free boot strategy (HLE).
- [ ] Vulkan rendering backend.
- [ ] Signed installers (.msi/.dmg/.deb) — only the unsigned app-image exists so far.
- [ ] Android packaging strategy.
- [ ] Metal backend groundwork.
- [ ] Official verbatim GPL-3.0 license text (currently a placeholder notice in `LICENSE`).

## Immediate recommended next steps

1. **Verify `./gradlew :app-javafx:jpackageImage` actually works on the real project** (not yet confirmed outside a trivial smoke test) — check the generated app-image launches and shows the JavaFX window correctly.
2. Wire `core-cpu-sh4`'s future SH-4 interpreter to `core-system`'s `SystemBus` as the concrete `Bus` implementation.
3. Extend disc reading to CUE/BIN using the same track-list-plus-sector-read approach as `GdiImage`.
