# Project Status

*Last updated: 2026-07-23 (SH-4 interpreter: delay slots implemented for BRA). Update this file whenever a contribution meaningfully changes what's implemented — see `CONTRIBUTING.md`.*

## Current state: bootstrap complete; system bus, disc reading, native packaging, and first CPU core work implemented

Real emulation infrastructure now spans four areas: the system memory bus, Dreamcast disc image format detection/reading, native app packaging, and — as of this update — the beginning of the SH-4 CPU interpreter.

### Done so far

- [x] Project charter, principles, repository structure, contribution rules, AI guidance, issue/PR templates.
- [x] Repository published publicly on GitHub (github.com/Gih-pt/dreamjemu).
- [x] Gradle wrapper committed; multi-module build verified successful (Linux, Java 21, Gradle 8.7).
- [x] Conservative `gradle.properties` committed for low-RAM machines.
- [x] Placeholder JavaFX window (`app-javafx`) verified to launch and display correctly.
- [x] CI verified on GitHub Actions: `build.yml` passes on `windows-latest`/`macos-latest`/`ubuntu-latest`. `nightly.yml`/`release.yml` invalid-YAML bug fixed and confirmed working.
- [x] `core-system`: memory map and system bus implemented and tested (7 JUnit tests passing).
- [x] `core-gdrom`: disc image format detection implemented and tested (8 JUnit tests) plus GDI parsing/sector reading implemented and tested (7 JUnit tests). 15 tests total in `core-gdrom`.
- [x] `docs/DEPENDENCIES.md` added: every third-party dependency, its purpose, and GPLv3 license-compatibility check.
- [x] `CONTRIBUTING.md` requires: mandatory AI-usage disclosure (yes/no) on every PR, keeping `docs/STATUS.md` / `docs/ROADMAP.md` / `CHANGELOG.md` current (with dates), and updating `docs/DEPENDENCIES.md` when dependencies change.
- [x] `app-javafx`: native app-image packaging via `jpackage` — implemented AND verified working end-to-end (bundled Java runtime, native launcher, confirmed running standalone on Linux). Fixed a "JavaFX runtime components are missing" launcher issue along the way (see CHANGELOG).
- [x] **`core-cpu-sh4`: SH-4 interpreter bring-up — first testable instruction subset implemented and tested.**
  - `Sh4Cpu`: register file (R0-R15), PC, PR, a T-flag-only status register, and a `step()` fetch-decode-execute loop against the generic `Bus` interface from `core-system` (deliberately NOT coupled to `SystemBus`/`DreamcastAddressMap` — the interpreter should work against any Bus implementation).
  - Implements 12 instructions so far: `NOP`, `MOV #imm,Rn`, `MOV Rm,Rn`, `ADD #imm,Rn`, `ADD Rm,Rn`, `SUB Rm,Rn`, `CMP/EQ Rm,Rn`, `CMP/EQ #imm,R0`, `BT`, `BF`, `BRA`, `MOV.L Rm,@Rn`/`MOV.L @Rm,Rn`. Everything else throws `UnsupportedOperationException` with the offending opcode and PC, by design (gaps are loud, not silently wrong).
  - **Delay slots are now implemented for `BRA`** (the only delayed-branch instruction implemented so far): the instruction at `PC+2` executes before the jump takes effect, matching real SH-4 hardware. Placing a branch instruction in a delay slot (illegal on real hardware) throws `IllegalStateException`, matching the "illegal slot instruction" concept rather than silently misbehaving. `BT`/`BF` remain correctly non-delayed (they never have a delay slot on real hardware either). This pattern is ready to extend to `BSR`/`JMP`/`JSR`/`RTS`/`RTE` once those are implemented.
  - 16 JUnit tests passing (`./gradlew :core-cpu-sh4:test`), covering every implemented instruction individually, delay-slot execution order, illegal-slot-instruction detection, plus one integration test: a hand-assembled loop program that sums 5+4+3+2+1 into a register using real conditional branching, then stores the result to memory and reads it back — 25 CPU steps, verified against both the register value and the stored memory content.

### Not started yet

- [ ] SH-4: the rest of the instruction set (delay slots are now handled for BRA; BSR/JMP/JSR/RTS/RTE aren't implemented yet, but can reuse the same delay-slot pattern), MMU, caches, exceptions/interrupts.
- [ ] Wiring `Sh4Cpu` to `core-system`'s real `SystemBus` (tested so far only against a trivial in-module test Bus).
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

1. Wire `Sh4Cpu` to `core-system`'s real `SystemBus`, and write an integration test that exercises real Dreamcast physical addresses (main RAM, and the VRAM/AICA placeholder regions).
2. Extend disc reading to CUE/BIN using the same track-list-plus-sector-read approach as `GdiImage`.
3. Grow the SH-4 instruction set (logic ops AND/OR/XOR, shifts, more addressing modes for MOV) and, when a delayed subroutine-call instruction (BSR/JSR) is added, reuse the delay-slot pattern already built for BRA.
