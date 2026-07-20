# Architecture

High-level module breakdown for the Gradle multi-module build. Module names below are the intended Gradle project names.

```
dreamjemu/
├── app-javafx/        # Desktop UI shell: library, saves, covers, settings, input, debug tools, i18n
├── common/             # Shared utilities, logging, config, event bus
├── core-cpu-sh4/       # Hitachi/Renesas SH-4 CPU emulation (interpreter, later dynarec)
├── core-gpu-pvr2/      # PowerVR CLX2 (PVR2) tile-based GPU emulation
├── core-aica/          # Yamaha AICA sound processor emulation
├── core-maple/         # Maple bus: controllers, VMU, other peripherals
├── core-gdrom/         # Disc image handling: format detection (GDI/CDI/CHD/CUE-BIN), sector reading
├── core-system/        # Memory map, system bus, scheduler tying CPU/GPU/AICA/Maple/GD-ROM together, BIOS-free HLE boot
├── render-vulkan/      # Vulkan rendering backend (LWJGL bindings) — the only supported production backend
└── render-metal/       # macOS/arm64 Metal backend — abstraction/stub only in early phases
```

## Design principles

- **No module may depend on a BIOS/firmware dump.** `core-system` is responsible for HLE (High-Level Emulation) boot logic that replaces what a real BIOS would do, without needing the original.
- **Rendering is abstracted behind a common interface** implemented by `render-vulkan` (and, later, `render-metal`), so `core-gpu-pvr2` never talks to a graphics API directly. This is what makes the early Metal-abstraction groundwork possible without committing to a full Metal implementation.
- **`app-javafx` never touches emulation internals directly** — it talks to `core-system` (and the debugging/diagnostics surface it exposes) through a stable API, so the UI and the emulation core can evolve independently.
- **Accuracy-sensitive modules (`core-cpu-sh4`, `core-gpu-pvr2`, `core-aica`) should be independently testable** against instruction/behavior test suites, without needing the full system or a JavaFX UI running.
- **OpenGL must never appear as a dependency anywhere in this tree.** Vulkan (via LWJGL) is the only supported graphics API, permanently.

## UI structure (`app-javafx`)

- Library view: game list, cover art, format detection results.
- Save manager: VMU/memory card contents, save states.
- Settings: resolution, language (i18n resource bundles under `app-javafx/src/main/resources/i18n`), graphics adapter selection, upscaling (FSR-class), FPS display/limit, emulation speed, online/network configuration.
- Input/controller mapping.
- Debug/diagnostic tools: CPU/GPU state inspection, log viewer, frame stepping — built to help identify emulation bugs, not just for end users.

## Build & release

- Gradle multi-module build, one artifact per platform (Windows/Linux/macOS/Android), produced via CI (`.github/workflows`).
- **Nightly** builds: automatic from the main branch's latest commits.
- **Stable** builds: cut periodically from nightly builds once they've proven reasonably solid; more polished and tested.
