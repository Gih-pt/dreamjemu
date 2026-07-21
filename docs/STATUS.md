# Project Status

*Last updated: 2026-07-20 (Gradle build + JavaFX shell verified working end-to-end). Update this file whenever a contribution meaningfully changes what's implemented — see `CONTRIBUTING.md`.*

## Current state: bootstrap complete, no emulation logic yet

The project scaffold (structure, documentation, contribution rules, issue/PR templates, CI skeleton) is in place, **and it has now been verified end-to-end**: the Gradle multi-module build compiles successfully, and the placeholder JavaFX shell (`app-javafx`) launches and displays its bootstrap window correctly on Linux (tested with Java 21, Gradle 8.7).

No hardware emulation code exists yet — this is intentional, per the project's stated priority on structure and accuracy first.

### Done so far

- [x] Project charter and principles defined (non-commercial, no piracy, GPLv3, AI-contribution-friendly).
- [x] Repository structure and module boundaries defined (`docs/ARCHITECTURE.md`).
- [x] Contribution rules and PR requirements defined (`CONTRIBUTING.md`).
- [x] AI contribution and review guidance defined (`docs/AI_CONTRIBUTIONS.md`).
- [x] Cross-chat/cross-AI continuation brief defined (`docs/AI_CONTINUATION.md`).
- [x] Issue templates for bug reports and compatibility reports.
- [x] Minimum requirements drafted (`docs/MINIMUM_REQUIREMENTS.md`).
- [x] License chosen (GPL v3.0) — placeholder notice in place; official verbatim text still to be swapped in (see `LICENSE`).
- [x] Repository published publicly on GitHub (github.com/Gih-pt/dreamjemu).
- [x] Gradle wrapper committed; multi-module build **verified successful** on a real machine (Linux, Java 21, Gradle 8.7).
- [x] Conservative `gradle.properties` committed (low memory footprint: `-Xmx768m`, no daemon, no parallelism) so the project builds reliably on modest hardware — this matters for a hobby project anyone should be able to contribute to.
- [x] Placeholder JavaFX window (`app-javafx`) **verified to launch and display correctly**.

### Not started yet

- [ ] Memory map / system bus implementation (`core-system`).
- [ ] SH-4 CPU core (interpreter first; JIT/dynarec later) — stub class exists (`core-cpu-sh4`), no logic implemented.
- [ ] PowerVR2 GPU core (tile-based deferred rendering emulation).
- [ ] AICA sound core.
- [ ] Maple bus (controllers, VMU) implementation.
- [ ] GD-ROM / disc image handling and format auto-detection (GDI, CDI, CHD, CUE/BIN) — stub classes exist (`core-gdrom`), no logic implemented.
- [ ] BIOS-free boot strategy (HLE boot ROM replacement).
- [ ] Vulkan rendering backend (LWJGL integration is declared in `render-vulkan/build.gradle.kts`, but no rendering code exists yet).
- [ ] CI pipelines actually producing Nightly/Stable release artifacts (workflows exist but are untested against a real build in GitHub Actions — should be verified next).
- [ ] Android packaging strategy.
- [ ] Metal backend groundwork (abstraction layer only, per project priorities).
- [ ] Official verbatim GPL-3.0 license text (currently a placeholder notice in `LICENSE`).

## Immediate recommended next steps

1. **Verify the GitHub Actions CI workflow actually passes** on the hosted runners now that the Gradle wrapper is committed (check the Actions tab).
2. Define the memory map and bus interfaces (`core-system`) so CPU/GPU/sound/Maple/GD-ROM modules have a shared contract to implement against.
3. Start the SH-4 interpreter with a small, testable instruction subset, validated against a known-good test ROM/instruction test suite (not full commercial games yet).
4. Stand up the disc-image loader with format detection for at least GDI and CUE/BIN, independent of any CPU/GPU work, since it's self-contained and easy to validate.
