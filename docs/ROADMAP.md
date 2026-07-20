# Roadmap

*Keep this in sync with `docs/STATUS.md`. Update whenever a contribution changes priorities or completes a milestone.*

Priorities are deliberately ordered. Please don't propose reordering phases without discussion — the project owner has set structure/accuracy before compatibility/performance, and modern rendering (Vulkan + upscaling) as a core priority rather than a later add-on.

## Phase 0 — Bootstrap (current)

- Repository, docs, contribution rules, CI skeleton. See `docs/STATUS.md`.

## Phase 1 — Core structure & accuracy

Goal: a correct, well-tested emulation core, even if slow and with no game fully playable yet.

- Working multi-module Gradle build (see `docs/ARCHITECTURE.md`).
- Memory map / system bus contracts shared across modules.
- SH-4 CPU interpreter, validated against instruction-level test suites.
- PowerVR2 GPU core: correct tile-based rendering pipeline (software or minimal Vulkan output first — correctness before speed).
- AICA sound core: correct sample generation before optimization.
- Maple bus: controller and VMU protocol emulation.
- GD-ROM: disc image format detection and reading (GDI, CDI, CHD, CUE/BIN at minimum), BIOS-free boot path (HLE).
- A minimal JavaFX shell able to load a disc image and show CPU/GPU debug state — this doubles as the first debugging tool.

## Phase 2 — Compatibility & performance

Goal: a broad range of games boot and run correctly, at acceptable speed.

- Expand instruction/hardware edge-case coverage driven by real compatibility reports (see the compatibility report issue template).
- Performance work: interpreter optimization, then a JIT/dynarec for the SH-4 if warranted.
- Save state support.
- VMU/save management UI in JavaFX.
- Expanded automated regression testing (tracking known-good games' behavior over time).

## Phase 3 — Modern rendering & UX (parallel priority, not deferred)

This phase's groundwork should start as early as practical, in parallel with Phase 1/2 work on the GPU core, not after Phase 2 finishes.

- Full Vulkan rendering backend via LWJGL (this is the only supported graphics API — no OpenGL, ever).
- Upscaling support (FSR-class spatial/temporal upscalers).
- Resolution scaling above native Dreamcast output.
- Graphics adapter selection UI.
- FPS display/limiting/uncapping, emulation speed control.
- JavaFX UI: game library with covers, settings screens (resolution, language, graphics adapter, upscaling, FPS, speed, online/network config), controller mapping, debugging/diagnostic tools (state inspection, logging, frame stepping).
- i18n groundwork: English and Portuguese first, structured so more languages can be added without code changes (resource-bundle based).

## Phase 4 — Multi-platform expansion

- Windows and Linux first-class support (initial target).
- macOS (x86_64) support.
- **Metal backend and full Apple Silicon/arm64 support** — planned, but *not* an initial priority. Early abstraction-layer groundwork for this (so the rendering backend interface doesn't assume Vulkan-only forever) is one of the project's **initial** milestones, even though the full Metal implementation is not.
- Android packaging and touch/controller input handling.

## Phase 5 — Online & polish

- Online/network play configuration and infrastructure.
- Nightly → Stable promotion process maturing (see `.github/workflows`).
- Broader localization beyond English/Portuguese, contributed via the i18n resource system.

## Next steps (most immediate, see `docs/STATUS.md` for details)

1. Working Gradle build + minimal JavaFX shell.
2. Memory map / bus contracts.
3. SH-4 interpreter bring-up with test-suite validation.
4. Disc image loader with format auto-detection.
