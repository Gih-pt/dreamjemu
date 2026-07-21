# AI Continuation Brief — DreamJEmu

Paste this whole document (or link to its raw GitHub URL) into a new chat with any AI assistant to bring it up to speed on this project. It is meant to be self-contained.

## What this project is

**DreamJEmu** is a hobby, non-commercial, fully open-source Sega Dreamcast emulator written in **Java**, using **JavaFX** for the desktop management UI and **Vulkan** (via LWJGL) as the — only — supported graphics API. It requires no BIOS dump or any file extracted from real Dreamcast hardware. It targets Windows, Linux, macOS, and Android, with a Metal backend and full Apple Silicon/arm64 support planned but not an initial priority.

The project owner started this for fun, will never monetize it in any way, does not support piracy, and has extensively used AI to bootstrap its structure and documentation — and continues to welcome AI-assisted contributions under the same rules as human ones.

## Non-negotiable constraints (do not suggest violating these)

1. **No BIOS / firmware / original console file dependency**, ever, for any feature.
2. **Vulkan only.** No OpenGL, now or in any future plan.
3. **No monetization of any kind** — no ads, donations, sponsorships, paid tiers, telemetry sale.
4. **No piracy support** — no distributing or linking to games/BIOS/copyrighted material.
5. **GNU GPL v3.0** license, fully public repository.
6. **Contribution language is English**; project docs are English-first, Portuguese second; the UI itself is built for easy addition of further languages later.
7. **Every PR must disclose AI usage explicitly — "yes" or "no" — not just when AI was used.**
8. **`docs/STATUS.md`, `docs/ROADMAP.md`, and `CHANGELOG.md` must be updated (with current dates) in the same PR** whenever a contribution is impactful enough to change what's implemented or planned next. New dependencies must be added to `docs/DEPENDENCIES.md` with a GPLv3 compatibility check.
9. Metal/arm64 macOS support: only its early abstraction groundwork is an initial-phase goal — do not treat a full Metal backend as near-term scope.

## Priority order (do not reorder without the project owner's explicit direction)

1. Core emulator **structure and hardware accuracy** (SH-4 CPU, PowerVR2 GPU, AICA sound, Maple bus, GD-ROM/disc handling, memory map) — HLE-based, no BIOS needed.
2. **Compatibility and performance** across a broad game library.
3. **Modern rendering features** as a core priority, not an afterthought: Vulkan rendering pipeline, upscaling (FSR-class), higher internal resolutions, modern post-processing.
4. **Multi-platform reach**: Windows/Linux first-class, macOS next, Android also targeted, Apple Silicon/Metal groundwork early but not urgent.

## Repository layout to expect (see `docs/ARCHITECTURE.md` for the authoritative version)

- `app-javafx` — desktop UI: library/save/cover/controller/settings management, debugging tools, i18n.
- `core-cpu-sh4`, `core-gpu-pvr2`, `core-aica`, `core-maple`, `core-gdrom` — hardware emulation modules.
- `render-vulkan` — the only production rendering backend (LWJGL Vulkan bindings).
- `render-metal` — early-stage abstraction/stub only, not a functioning backend yet.
- `common` — shared utilities/logging.
- `docs/` — `STATUS.md` (what exists now), `ROADMAP.md` (recommended next steps), `ARCHITECTURE.md`, `AI_CONTRIBUTIONS.md`, `DEPENDENCIES.md` (every third-party dependency + GPLv3 compatibility check), `MINIMUM_REQUIREMENTS.md`, `pt/` (Portuguese docs).
- `.github/` — issue templates (bug report, compatibility report), PR template, CI workflows for Nightly/Stable builds across Windows/macOS/Linux (and Android tooling as it matures).

## How to actually pick up work

1. Fetch/read the live `docs/STATUS.md` and `docs/ROADMAP.md` from the repository (do not rely solely on this brief, which will go stale — those two files are the source of truth and are meant to be updated with every impactful contribution).
2. Follow `CONTRIBUTING.md` exactly: every PR must state what the change does, how, why it's necessary, and what game/test case validated it — in English.
3. Treat unsupported-hardware complaints (e.g. "doesn't work, my GPU has no Vulkan") and reports missing required fields as invalid, per `docs/MINIMUM_REQUIREMENTS.md`.

## Note to the assistant reading this

If you are an AI being asked to continue this project in a new conversation: ask the user for the current repository URL/state if you don't have it, don't assume prior chat context carried over, and re-derive the current status from the live `docs/STATUS.md` rather than from memory of this brief.
