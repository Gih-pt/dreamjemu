# DreamJEmu — A Java Dreamcast Emulator

*[Leia isto em Português](docs/pt/README.pt.md)*

> **Status:** 🚧 Early planning / bootstrap stage. No emulation core exists yet. See [`docs/STATUS.md`](docs/STATUS.md) for the current state and [`docs/ROADMAP.md`](docs/ROADMAP.md) for what comes next.

## What this is

DreamJEmu is a hobby, non-commercial, community-driven Sega Dreamcast emulator written in **Java**, with a **JavaFX** front-end and a **Vulkan**-based rendering backend. It is built openly, in public, with heavy use of AI assistance for both code and documentation, and it explicitly welcomes further AI-assisted contributions as long as they follow the same rules as human ones (see [`CONTRIBUTING.md`](CONTRIBUTING.md)).

It does **not** require a BIOS dump or any other copyrighted file extracted from a real Dreamcast console.

## Project principles (please read)

- **This project will never seek to make money.** No donations, no sponsorships, no ads, no paid tiers, ever. It exists for fun and for the learning experience.
- **This project does not support piracy.** It does not distribute games, BIOS files, or any copyrighted Sega/third-party material, and it will not link to sources of such material. Users are responsible for only using software they legally own.
- **Everything is public and free.** All source code, documentation, build artifacts, and project history live in this public repository under the license in [`LICENSE`](LICENSE) (GNU GPL v3.0).
- **AI contributions are welcome.** AI was extensively used to bootstrap this project's structure and documentation, and AI-generated pull requests are accepted on equal footing with human ones — provided they follow [`CONTRIBUTING.md`](CONTRIBUTING.md) to the letter.
- **No original console files are needed.** No BIOS, no flash ROM dump, nothing extracted from real hardware. If a feature ever appears to require one, that is treated as a bug in the emulator's HLE (High-Level Emulation) design, not an acceptable requirement for users.

## Goals, in priority order

1. **Structure and accuracy first.** A clean, well-documented, modular architecture that emulates the Dreamcast's hardware (SH-4 CPU, PowerVR2 GPU, AICA sound, Maple bus, GD-ROM) as correctly as possible, without needing BIOS/firmware dumps.
2. **Compatibility and performance second.** Once the core is accurate, broaden game compatibility and optimize for real-world speed.
3. **Modern graphical enhancements as a core priority (not an afterthought).** Native **Vulkan** rendering (OpenGL is explicitly out of scope, permanently), with upscaling support (e.g. **AMD FSR**), higher internal resolutions, and other modern rendering improvements, planned in from the start rather than bolted on later.
4. **Multi-platform from day one.** Windows, macOS, Linux, and Android are all target platforms. Full macOS/Apple Silicon (arm64) support, including a **Metal** rendering backend, is a planned goal — not an initial priority, but its foundational structure (abstraction layer, build targets) is one of the project's early milestones.

## Planned features

- **No BIOS / no original firmware required** to boot and play.
- **JavaFX desktop UI** for:
  - Game library management (multiple Dreamcast disc image formats — see below)
  - Save state and VMU/memory card save management
  - Game cover/art management
  - Input/controller configuration and mapping
  - Graphics settings: resolution, graphics adapter selection, upscaling (FSR and similar), frame rate limits/uncapping, emulation speed
  - Language selection (interface is designed for easy addition of new languages/locales)
  - Online/network play configuration
  - Built-in debugging and diagnostic tools to help identify emulation bugs (CPU/GPU state inspection, logging, frame stepping, etc.)
- **Format detection** for common Dreamcast disc image formats (GDI, CDI, CHD, CUE/BIN, and others as needed), without relying on any original console files.
- **Vulkan-only rendering backend.** OpenGL is not supported and will not be supported.
- **Upscaling support** (FSR-class spatial/temporal upscalers) as a first-class rendering feature, not a stretch goal.
- **Nightly and Stable release channels**, built and published automatically from this repository (see [`docs/RELEASES.md`](docs/RELEASES.md) once available, and the GitHub Actions workflows in [`.github/workflows`](.github/workflows)).

## Platforms

| Platform | Status |
|---|---|
| Windows (x86_64) | Planned / initial target |
| Linux (x86_64) | Planned / initial target |
| macOS (x86_64) | Planned |
| macOS (arm64 / Apple Silicon, Metal backend) | Planned, not an initial priority — early abstraction groundwork is an initial milestone |
| Android | Planned |

## Minimum requirements (draft — will evolve as the core matures)

See [`docs/MINIMUM_REQUIREMENTS.md`](docs/MINIMUM_REQUIREMENTS.md) for full details. In short:

- A 64-bit OS from the table above.
- A GPU and driver with **Vulkan 1.2+** support. This is a hard requirement — there is no OpenGL fallback and none is planned.
- Java 21+ compatible runtime (bundled with releases; not required to be pre-installed by end users).
- Reports of problems on hardware that does **not** meet these requirements (e.g. no Vulkan support) are considered invalid and will be closed — see [`docs/MINIMUM_REQUIREMENTS.md`](docs/MINIMUM_REQUIREMENTS.md).

## Technology choices

| Area | Choice | Notes |
|---|---|---|
| Language | Java (21+) | Core emulation, UI, and tooling |
| UI | JavaFX | Desktop management UI: library, saves, settings, controls, debugging tools |
| Graphics API | **Vulkan** | Via [LWJGL](https://www.lwjgl.org/) bindings. OpenGL is explicitly not supported, now or in the future. |
| Future graphics API (planned, non-priority) | Metal (macOS/arm64) | Initial abstraction layer is an early goal; full implementation is not |
| Build system | Gradle (multi-module) | See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) |
| License | GNU GPL v3.0 | See [`LICENSE`](LICENSE) |

See [`docs/DEPENDENCIES.md`](docs/DEPENDENCIES.md) for the full list of third-party dependencies, their purpose, and their license compatibility with GPLv3.

## Repository structure

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full breakdown of modules (CPU, GPU, sound, Maple bus, GD-ROM, rendering backends, UI, common utilities) and how they fit together.

## Getting started as a contributor

1. Read [`docs/STATUS.md`](docs/STATUS.md) to understand what currently exists.
2. Read [`docs/ROADMAP.md`](docs/ROADMAP.md) to see recommended next steps and pick something that matches an area flagged as needing work.
3. Read [`CONTRIBUTING.md`](CONTRIBUTING.md) carefully before opening a pull request — **PRs that don't follow it will be closed without merging**, regardless of code quality.
4. Check [`docs/DEPENDENCIES.md`](docs/DEPENDENCIES.md) before adding any new library or tool.
5. If you're an AI agent (or a human using one), also read [`docs/AI_CONTRIBUTIONS.md`](docs/AI_CONTRIBUTIONS.md) for contribution *and* review guidance specific to AI-assisted work.
6. If you want to pick up this project's context in a fresh chat with any AI assistant, see [`docs/AI_CONTINUATION.md`](docs/AI_CONTINUATION.md) — it's a self-contained brief written for that purpose.

## Reporting issues

There are two different report types, with different required information. Reports missing the required information, or reporting unsupported-hardware limitations (e.g. "doesn't work, my GPU has no Vulkan support"), are invalid and will be closed. See the issue templates for the exact fields required:

- **Compatibility report** (a game behaves incorrectly): must include the game tested, a log, the emulator version, and a description of the game's current behavior.
- **Bug report** (a general problem, not tied to a specific game): must include the emulator version, a log (when relevant), a description of the problem, and steps to reproduce it.

## License

DreamJEmu is licensed under the **GNU General Public License v3.0**. See [`LICENSE`](LICENSE) for the full text. This project, and everything in it, is and will always remain free and public.

## Disclaimer

This project is a personal hobby project. It does not accept donations or payments of any kind and never will. It does not condone or support piracy in any form, and it does not distribute copyrighted Sega/third-party material. Users are solely responsible for how they use this software and for owning any software they run with it.
