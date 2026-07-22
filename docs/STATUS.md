# Project Status

*Last updated: 2026-07-22 (core-gdrom GDI parsing and sector reading implemented and tested; CI nightly/release workflow YAML fixed). Update this file whenever a contribution meaningfully changes what's implemented — see `CONTRIBUTING.md`.*

## Current state: bootstrap complete; system bus, disc format detection, and GDI reading implemented; CI fully green

The project scaffold is in place and verified end-to-end. Three pieces of real emulation infrastructure now exist and are tested: the system memory bus, Dreamcast disc image format detection, and GDI parsing/sector reading. All three GitHub Actions workflows (Build, Nightly, Release) are syntactically valid; Build passes on every push across Windows/macOS/Linux.

### Done so far

- [x] Project charter, principles, repository structure, contribution rules, AI guidance, issue/PR templates.
- [x] Repository published publicly on GitHub (github.com/Gih-pt/dreamjemu).
- [x] Gradle wrapper committed; multi-module build verified successful (Linux, Java 21, Gradle 8.7).
- [x] Conservative `gradle.properties` committed for low-RAM machines.
- [x] Placeholder JavaFX window (`app-javafx`) verified to launch and display correctly.
- [x] **CI verified on GitHub Actions**: `build.yml` passes on `windows-latest`/`macos-latest`/`ubuntu-latest`. `nightly.yml` and `release.yml` had an invalid-YAML bug (unquoted `: ` inside a plain scalar `run:` value) that made GitHub reject them outright — fixed by switching those `run:` steps to block-scalar (`|`) style; both now parse and pass GitHub's workflow validation.
- [x] **`core-system`: memory map and system bus implemented and tested** (`Bus`/`MemoryRegion` interfaces, `RamRegion`, `UnmappedRegion`, `DreamcastAddressMap`, `SystemBus`; 7 JUnit tests passing).
- [x] **`core-gdrom`: disc image format detection implemented and tested** (`DiscImageDetector` — GDI, CDI, CHD, CUE/BIN; 8 JUnit tests passing).
- [x] **`core-gdrom`: GDI parsing and sector reading implemented and tested.**
  - `GdiTrackType` (audio/data), `GdiTrack` (parsed per-track record), `GdiImage` (parses a `.gdi` file's track list and reads sector data from the referenced track files, resolved relative to the `.gdi`'s own directory).
  - Handles multiple tracks with different files/sector sizes, locates the correct track for a given LBA, and reads the correct byte offset within that track's file.
  - 7 JUnit tests passing (`./gradlew :core-gdrom:test`), covering track-list parsing, sector reads from two different tracks/files, an out-of-range LBA, a mismatched track-count header, and a missing referenced track file (clear error message, not a silent failure).
  - Does not require or read any original console/BIOS file.
- [x] `docs/DEPENDENCIES.md` added: every third-party dependency, its purpose, and GPLv3 license-compatibility check.
- [x] `CONTRIBUTING.md` now requires: mandatory AI-usage disclosure (yes/no) on every PR, keeping `docs/STATUS.md` / `docs/ROADMAP.md` / `CHANGELOG.md` current (with dates) on every impactful PR, and updating `docs/DEPENDENCIES.md` when dependencies change.

### Not started yet

- [ ] SH-4 CPU core (interpreter) — stub class exists (`core-cpu-sh4`), can now be wired to the real `Bus` implementation from `core-system`.
- [ ] PowerVR2 GPU core.
- [ ] AICA sound core.
- [ ] Maple bus (controllers, VMU).
- [ ] CDI/CHD/CUE-BIN *reading* (only GDI reading exists so far; detection exists for all four formats).
- [ ] BIOS-free boot strategy (HLE).
- [ ] Vulkan rendering backend.
- [ ] Android packaging strategy.
- [ ] Metal backend groundwork.
- [ ] Official verbatim GPL-3.0 license text (currently a placeholder notice in `LICENSE`).

## Immediate recommended next steps

1. Wire `core-cpu-sh4`'s future SH-4 interpreter to `core-system`'s `SystemBus` as the concrete `Bus` implementation — the two foundational pieces (bus, disc reading) are now solid enough to build the CPU core against.
2. Extend disc reading to CUE/BIN (next-simplest format after GDI) using the same track-list-plus-sector-read approach as `GdiImage`.
3. Consider a small `core-gdrom` facade that picks the right reader (`GdiImage`, future `CueBinImage`, etc.) based on `DiscImageDetector`'s result, so `core-system`/the eventual boot sequence doesn't need to know about per-format classes.
