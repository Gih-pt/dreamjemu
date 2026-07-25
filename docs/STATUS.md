# Project Status

*Last updated: 2026-07-25 (core-gdrom: CUE/BIN sector reading implemented). Update this file whenever a contribution meaningfully changes what's implemented — see `CONTRIBUTING.md`.*

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
- [x] **`core-gdrom`: CUE/BIN sector reading implemented.**
  - `CueTrackMode`: the sector-size-bearing track modes seen in real CUE sheets (`AUDIO`, `MODE1/2048`, `MODE1/2352`, `MODE2/2048`, `MODE2/2336`, `MODE2/2352`), parsed case-insensitively from a TRACK line's mode token.
  - `CueTrack`: a resolved track record — global start LBA, sector count, mode, and the `.bin` file name/byte offset where its data begins — analogous to `GdiTrack` but with the LBA and sector count *computed* rather than read directly, since CUE sheets don't declare either.
  - `CueBinImage`: parses `FILE "name" BINARY` / `TRACK NN <mode>` / `INDEX 01 MM:SS:FF` lines (ignoring metadata-only lines like `REM`/`TITLE`/`PREGAP`/etc.), resolves `.bin` files relative to the `.cue` file's directory, and implements `readSector(long lba, byte[] dest)` following the exact same lazy-open-per-file pattern as `GdiImage`.
  - A track's sector count is derived, not declared: the gap to the next track's `INDEX 01` within the same file (or the remaining bytes in the file, for the last track referencing it, divided by that track's sector size). Global LBA is the running total of every earlier track's sector count, so track 1 starts at LBA 0 — consistent with the whole-disc LBA numbering already used by `GdiImage`.
  - **The exact `MM:SS:FF` → frame conversion (no 150-sector/2-second lead-in offset for CUE-sheet-relative indexes, unlike whole-disc absolute MSF addressing) was verified against public CUE sheet format documentation before implementation** (see the CHANGELOG entry for sources) — this confirmed the file-relative index value is directly usable as a sector count within its `FILE`, with no adjustment needed.
  - Supports both common real-world layouts: multiple tracks sharing one `.bin` file, and one `.bin` file per track.
  - Covers error cases: missing `INDEX 01`, a `TRACK` line before any `FILE` line, an unsupported `FILE` type (only `BINARY` is supported — no audio-file-based cue sheets), a malformed/unknown track mode, a missing referenced `.bin` file, and a file size that isn't a whole number of sectors for the mode declared.
  - 10 new JUnit tests (`CueBinImageTest`); together with the existing 15, `core-gdrom` now has 25 tests. **Confirmed passing** with `./gradlew :core-gdrom:test` on a real machine (JDK 21, Gradle 8.7) — see the CHANGELOG entry, including a real parser bug the test run caught and fixed before this was true.
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
- [x] **`core-cpu-sh4`: added `JMP`, `MOV.B`/`MOV.W`, and `NOT`/`NEG`.**
  - `JMP @Rn` — delayed unconditional jump through a register, reusing the same "read target before delay slot" discipline as `JSR`, but correctly does **not** touch `PR` (unlike `JSR`) — tested with a `PR` sentinel value to confirm.
  - `MOV.B`/`MOV.W` load/store — byte and 16-bit word memory access (previously only `MOV.L`, 32-bit, existed), both sign-extending on load. Tested with values that would look wrong if sign-extension were missed.
  - `NOT Rm,Rn` (bitwise complement) and `NEG Rm,Rn` (two's-complement negation).
  - `RTE` (return from exception) is deliberately **not** implemented yet — it needs `SSR`/`SPC` (saved status register / saved PC) and real exception/interrupt-mode state that don't exist in this interpreter yet; implementing it now would just be a stub with no meaningful behavior. Tracked alongside MMU/exceptions in "not started yet" below.
  - 7 new JUnit tests; 41 tests total in `core-cpu-sh4`.
- [x] **`core-cpu-sh4`: added multiply (`MUL.L`/`MULS.W`/`MULU.W`/`DMULS.L`/`DMULU.L`) and divide (`DIV0U`/`DIV0S`/`DIV1`) instructions, plus `ROTCL`.**
  - Added `mach`/`macl` registers (multiply-accumulate result) and the `Q`/`M` division flags, alongside the existing `T` flag.
  - `MUL.L` truncates to 32 bits (`MACL` only); `MULS.W`/`MULU.W` operate on the low 16 bits of each register (sign- vs zero-extended respectively — tested explicitly, since mixing these up is a classic bug); `DMULS.L`/`DMULU.L` produce a full 64-bit result in `MACH:MACL`, implemented via 64-bit Java `long` arithmetic rather than the reference 32-bit-limb algorithm (simpler, mathematically equivalent).
  - `DIV0U`/`DIV0S`/`DIV1` implement the SH-4's bit-serial division primitive. **The exact `DIV1` semantics (including subtle unsigned-comparison behavior for the internal borrow/carry check) were verified against a public SH instruction set reference before implementation** — this caught two inverted-ternary bugs during development that would otherwise have silently produced wrong division results in about half of all cases.
  - **Verified with the actual documented 32-bit unsigned division routine** (`DIV0U` + 32×`{ROTCL; DIV1}` + a final `ROTCL`, matching the reference example) run end-to-end for two different dividend/divisor pairs, each checked against the independently-computed correct quotient (`100/7=14`, `1000000/3=333333`) — a much stronger correctness signal than testing `DIV1` in isolation alone, since a remaining subtle bug would very likely make a real 32-iteration division produce a wrong answer.
  - `RTE` remains deliberately unimplemented (see the earlier entry above).
  - 11 new JUnit tests, including the full division routine; 52 tests total in `core-cpu-sh4`.

### Not started yet

- [ ] SH-4: the rest of the instruction set (logic/shift/subroutine calls/JMP/byte-word memory access/multiply/divide now covered; still missing: `RTE` — needs SSR/SPC and exception-mode state — MMU, caches, exceptions/interrupts, more addressing modes), delay slots are handled for all currently-implemented delayed branches (`BRA`/`BSR`/`JSR`/`JMP`/`RTS`).
- [ ] PowerVR2 GPU core.
- [ ] AICA sound core.
- [ ] Maple bus (controllers, VMU).
- [ ] CDI/CHD *reading* (GDI and CUE/BIN reading now exist; detection exists for all four formats).
- [ ] BIOS-free boot strategy (HLE).
- [ ] Vulkan rendering backend.
- [ ] Signed installers (.msi/.dmg/.deb) — only the unsigned app-image exists so far.
- [ ] Android packaging strategy.
- [ ] Metal backend groundwork.
- [ ] Official verbatim GPL-3.0 license text (currently a placeholder notice in `LICENSE`).

## Immediate recommended next steps

1. Extend disc reading to CDI and CHD — GDI and CUE/BIN reading are now both done, following the same track-list-plus-sector-read approach; CHD in particular will need a compressed-hunk-reading dependency (see `docs/DEPENDENCIES.md` before adding one).
2. Start sketching the BIOS-free HLE boot sequence — the interpreter now has arithmetic (including multiply/divide), logic, all memory access sizes, branching, and subroutine calls, enough to express real, non-trivial control flow.
3. Pivot toward PowerVR2/AICA/Maple groundwork per `docs/ROADMAP.md` Phase 1 — `core-cpu-sh4`'s instruction coverage is now broad enough that further growth can be driven by whatever HLE/boot code actually needs, rather than added speculatively.
