# Project Status

*Last updated: 2026-07-20 (project bootstrap). Update this file whenever a contribution meaningfully changes what's implemented — see `CONTRIBUTING.md`.*

## Current state: bootstrap / pre-alpha

No emulation core exists yet. What exists right now is the **project scaffold**: repository structure, documentation, contribution rules, issue/PR templates, and CI skeleton. This is intentional — the project is starting from a solid structural and procedural foundation before any hardware emulation code is written, per the project's stated priority on structure and accuracy first.

### Done so far

- [x] Project charter and principles defined (non-commercial, no piracy, GPLv3, AI-contribution-friendly).
- [x] Repository structure and module boundaries defined (`docs/ARCHITECTURE.md`).
- [x] Contribution rules and PR requirements defined (`CONTRIBUTING.md`).
- [x] AI contribution and review guidance defined (`docs/AI_CONTRIBUTIONS.md`).
- [x] Cross-chat/cross-AI continuation brief defined (`docs/AI_CONTINUATION.md`).
- [x] Issue templates for bug reports and compatibility reports.
- [x] Minimum requirements drafted (`docs/MINIMUM_REQUIREMENTS.md`).
- [x] License chosen (GPL v3.0).

### Not started yet

- [ ] Gradle multi-module build actually producing runnable artifacts.
- [ ] SH-4 CPU core (interpreter first; JIT/dynarec later).
- [ ] Memory map / system bus implementation.
- [ ] PowerVR2 GPU core (tile-based deferred rendering emulation).
- [ ] AICA sound core.
- [ ] Maple bus (controllers, VMU) implementation.
- [ ] GD-ROM / disc image handling and format auto-detection (GDI, CDI, CHD, CUE/BIN).
- [ ] BIOS-free boot strategy (HLE boot ROM replacement).
- [ ] Vulkan rendering backend (LWJGL integration).
- [ ] JavaFX shell application (even a minimal window that can load a disc image).
- [ ] CI pipelines actually producing Nightly/Stable builds for each platform.
- [ ] Android packaging strategy.
- [ ] Metal backend groundwork (abstraction layer only, per project priorities).

## Immediate recommended next steps

See `docs/ROADMAP.md` for the fuller picture; the very next concrete steps are:

1. Set up a working Gradle multi-module build that compiles empty/stub modules and launches a minimal JavaFX window.
2. Define the memory map and bus interfaces (`common`/`core-*` module contracts) so CPU/GPU/sound/Maple/GD-ROM modules have a shared contract to implement against.
3. Start the SH-4 interpreter with a small, testable instruction subset, validated against a known-good test ROM/instruction test suite (not full commercial games yet).
4. Stand up the disc-image loader with format detection for at least GDI and CUE/BIN, independent of any CPU/GPU work, since it's self-contained and easy to validate.
