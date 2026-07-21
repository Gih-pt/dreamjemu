# Project Status

*Last updated: 2026-07-21 (core-gdrom disc image format detection implemented and tested). Update this file whenever a contribution meaningfully changes what's implemented — see `CONTRIBUTING.md`.*

## Current state: bootstrap complete; system bus and disc format detection implemented

The project scaffold is in place and verified end-to-end (Gradle build, JavaFX shell launching correctly). Two pieces of real emulation infrastructure now exist and are tested: the system memory bus, and Dreamcast disc image format detection.

### Done so far

- [x] Project charter, principles, repository structure, contribution rules, AI guidance, issue/PR templates, CI skeleton.
- [x] Repository published publicly on GitHub (github.com/Gih-pt/dreamjemu).
- [x] Gradle wrapper committed; multi-module build verified successful (Linux, Java 21, Gradle 8.7).
- [x] Conservative `gradle.properties` committed for low-RAM machines.
- [x] Placeholder JavaFX window (`app-javafx`) verified to launch and display correctly.
- [x] **`core-system`: memory map and system bus implemented and tested** (`Bus`/`MemoryRegion` interfaces, `RamRegion`, `UnmappedRegion`, `DreamcastAddressMap`, `SystemBus`; 7 JUnit tests passing, including RAM mirroring and SH-4 cache-area address masking).
- [x] **`core-gdrom`: disc image format detection implemented and tested.**
  - `DiscImageDetector.detect(Path)` identifies GDI, CDI, CHD, and CUE/BIN images using a combination of file extension and structural content checks (GDI/CUE via text structure, CHD via its "MComprHD" magic header, CDI via its trailer version marker), falling back to content probing when the extension is missing, wrong, or doesn't match the actual content.
  - 8 JUnit tests passing (`./gradlew :core-gdrom:test`), covering each format, a mismatched-extension case, garbage content with a matching extension, an unrelated file, and a non-existent file.
  - Does not require or read any original console/BIOS file — operates purely on the user-provided disc image.

### Not started yet

- [ ] SH-4 CPU core (interpreter) — stub class exists (`core-cpu-sh4`), can now be wired to the real `Bus` implementation from `core-system`.
- [ ] PowerVR2 GPU core.
- [ ] AICA sound core.
- [ ] Maple bus (controllers, VMU).
- [ ] GD-ROM: actual sector/track *reading* from detected images (detection only, so far) — parsing GDI track lists, reading BIN/RAW sector data, etc.
- [ ] BIOS-free boot strategy (HLE).
- [ ] Vulkan rendering backend.
- [ ] CI pipelines verified against a real GitHub Actions run (should be checked).
- [ ] Android packaging strategy.
- [ ] Metal backend groundwork.
- [ ] Official verbatim GPL-3.0 license text (currently a placeholder notice in `LICENSE`).

## Immediate recommended next steps

1. Extend `core-gdrom` to actually read track/sector data from a detected image (starting with GDI, since it's the simplest to parse), now that format detection exists.
2. Wire `core-cpu-sh4`'s future SH-4 interpreter to `core-system`'s `SystemBus` as the concrete `Bus` implementation.
3. Verify the GitHub Actions CI workflow passes on hosted runners now that the wrapper and modules with real tests exist.
