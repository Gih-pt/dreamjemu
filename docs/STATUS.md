# Project Status

*Last updated: 2026-07-24 (SH-4 interpreter: added BSR/JSR/RTS subroutine call/return, all with correct delay-slot semantics). Update this file whenever a contribution meaningfully changes what's implemented — see `CONTRIBUTING.md`.*

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
- [x] **`core-cpu-sh4`: `Sh4Cpu` wired to core-system's real `SystemBus`.**
  - Previously only tested against a trivial in-module `SimpleTestBus`; now has a dedicated integration test suite (`Sh4CpuSystemBusIntegrationTest`) running the same hand-assembled loop program against the real `SystemBus`/`DreamcastAddressMap`.
  - Covers: execution against real main RAM (with results read back through the bus, not just the register file), identical execution when booted through an SH-4 cache-area mirror (0xA0000000-based addressing, confirming `SystemBus`'s address masking works correctly at the CPU level too), and safe read/write interaction with the still-unmapped VRAM placeholder region (writes silently discarded, reads return 0 — no crash), confirming early bring-up code can safely poke at not-yet-implemented peripherals.
  - Shared instruction encoders (`Sh4Asm`) extracted out of `Sh4CpuTest` so both test classes stay in sync with the interpreter's actual instruction formats. 3 new integration tests; 19 tests total in `core-cpu-sh4`.
- [x] **`core-cpu-sh4`: added logic (`AND`/`OR`/`XOR`) and shift (`SHLL`/`SHLR`/`SHAL`/`SHAR`) instructions.**
  - Register-register and R0-immediate forms of `AND`/`OR`/`XOR`. The immediate forms are **zero-extended** (unlike `MOV`/`ADD`/`CMP`'s sign-extended immediates) — tests specifically check this distinction, since getting it wrong is a classic and easy-to-miss interpreter bug.
  - `SHLL`/`SHAL` (shift left, functionally identical on real hardware) and `SHLR` (logical/zero-fill) vs `SHAR` (arithmetic/sign-fill) shift-right, each setting the T flag from the bit shifted out.
  - 10 new JUnit tests; 29 tests total in `core-cpu-sh4`.
- [x] **`core-cpu-sh4`: added `BSR`/`JSR`/`RTS` subroutine call/return, all with correct delay-slot semantics.**
  - `BSR label` and `JSR @Rn` set `PR` to the return address and jump to their target (a PC-relative displacement for `BSR`, a register value for `JSR`) after executing their delay slot, reusing the same delay-slot mechanism built for `BRA`. `RTS` jumps to `PR` after its own delay slot.
  - `JSR` specifically reads its target register **before** the delay slot executes, matching real hardware — verified by a test where the delay slot instruction itself overwrites that register, confirming the interpreter uses the old value for the jump target while still executing the delay slot's effect.
  - Illegal-slot-instruction detection (a branch inside a delay slot) now also covers `BSR`/`JSR`/`RTS`, not just `BRA`.
  - 5 new JUnit tests, including a full "call a subroutine, run its body, return" round-trip integration test; 34 tests total in `core-cpu-sh4`.

### Not started yet

- [ ] SH-4: the rest of the instruction set (logic/shift/subroutine calls now covered; still missing: `JMP`/`RTE`, MMU, caches, exceptions/interrupts, more addressing modes for MOV, multiply/divide), delay slots are handled for all currently-implemented delayed branches (`BRA`/`BSR`/`JSR`/`RTS`) and can be reused for `JMP`/`RTE`.
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

1. Extend disc reading to CUE/BIN using the same track-list-plus-sector-read approach as `GdiImage`.
2. Start sketching the BIOS-free HLE boot sequence — the interpreter now has enough (arithmetic, memory access, branching, and subroutine calls) to express real control flow.
3. Consider `JMP`/`RTE` next for `core-cpu-sh4` (same delay-slot pattern again), or pivot toward PowerVR2/AICA/Maple groundwork per `docs/ROADMAP.md` Phase 1.
