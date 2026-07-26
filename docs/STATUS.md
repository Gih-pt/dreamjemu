# Project Status

*Last updated: 2026-07-26 (core-gdrom: CDI sector reading implemented, fixing a real detector bug along the way; IP.BIN boot header parsing added as the first HLE boot sketch step; new app-cli module for GUI-free disc image testing). Update this file whenever a contribution meaningfully changes what's implemented — see `CONTRIBUTING.md`.*

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
- [x] **`core-gdrom`: CDI (DiscJuggler) sector reading implemented.**
  - `CdiTrackMode`: the three CDI track-mode codes (`CDDA`/audio, `DATA`/Mode 1, `MULTI`/Mode 2), each paired with a separate "sector size code" field that together determine the actual sector size.
  - `CdiTrack`: a resolved track — unlike `CueTrack`, nothing here is derived; CDI's binary header declares each track's global LBA and sector count directly.
  - `CdiImage`: parses a CDI file's 8-byte trailer (`{version, header_offset}`) at the very end of the file, locates and parses its binary header (session count, then each session's track count and per-track records), and reads sectors from the single monolithic `.cdi` file itself (unlike GDI/CUE, CDI doesn't reference separate track files). Supports CDI v2/v3/v3.5, including v3.5's backward-measured header offset.
  - **Bug found and fixed in `DiscImageDetector` during this work**: `looksLikeCdi` was checking the file's literal last 4 bytes as the version marker, but the real trailer is `{version(4 bytes), header_offset(4 bytes)}` in that order — so the true last 4 bytes are the arbitrary `header_offset`, not the version marker, which sits 4 bytes earlier. Fixed the detector to check the correct window and corrected the version constants to match the sourced reference. Added a regression test confirming a `header_offset` value that happens to look like a version marker isn't misread as one.
  - 9 new JUnit tests (`CdiImageTest`) plus 1 regression test in `DiscImageDetectorTest`; `core-gdrom` now has 35 tests. **Independently re-verified in a sandbox** (no JDK/Gradle available there for the real JUnit suite) by reconstructing the exact same synthetic-file-building logic as a standalone program and running it directly against the real `CdiImage`/`DiscImageDetector` classes: single- and multi-session track parsing, sector reads (including a session/track spanning a non-zero LBA), and the detector's corrected trailer check all confirmed working (13/13 checks passed) — still needs a real `./gradlew :core-gdrom:test` run to be fully validated, same as everything else awaiting that confirmation.
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
- [x] **`core-gdrom`: `IpBinHeader` — parses a Dreamcast disc's IP.BIN boot header (hardware ID, product info, boot filename, game title). First concrete step of the BIOS-free HLE boot sequence.**
  - Reads the 256-byte descriptive header at the start of a disc's data track: hardware/maker ID, device info, area symbols, peripherals, product number/version, release date, and — most importantly for booting — the boot filename (typically `1ST_READ.BIN`) that a real BIOS would load and jump to.
  - **Field byte offsets could not be sourced from a single authoritative reference** (a direct fetch of an existing open-source emulator's source file was blocked by that site's robots.txt); instead they were reconstructed by cross-referencing multiple independent public sources (real debug log output from three different games via an open-source HLE BIOS implementation, plus a separately-confirmed region-flag byte offset from an unrelated tool) and checked for internal consistency (field widths sum to exactly 0x80 bytes). **This is flagged as provisional and not yet validated against a real disc image byte-for-byte** — an important next step for whoever picks this up, tracked below.
  - 5 new JUnit tests against a hand-built synthetic header (fictional test data, not from any real game), covering field parsing, padding trimming, hardware-ID validation, and a too-short-input error case.
- [x] **New module `app-cli`: a simple command-line disc image inspector, usable while `app-javafx`'s GUI is still early.**
  - Detects a disc image's format, loads it (GDI or CUE/BIN so far), locates its data track, reads the first sector, and prints the parsed `IpBinHeader` — hardware ID validity, product number, boot filename, game title, etc.
  - Correctly handles both raw 2352-byte sectors (skipping the 16-byte sync/header preamble to reach the actual 2048 bytes of user data) and plain 2048-byte sectors.
  - **Does not run the game yet** — that needs ISO9660 filesystem parsing (to locate the boot file named in IP.BIN within the disc's file listing), RAM loading, and SH-4 boot wiring, none of which exist yet. This tool is explicitly scoped as an inspection/smoke-test aid, not a game runner, and says so in its own output.
  - Run with: `./gradlew :app-cli:run --args="path/to/game.gdi"` (or `.cue`). Verified end-to-end against a hand-built synthetic GDI image with a fake IP.BIN header.

### Not started yet

- [ ] SH-4: the rest of the instruction set (logic/shift/subroutine calls/JMP/byte-word memory access/multiply/divide now covered; still missing: `RTE` — needs SSR/SPC and exception-mode state — MMU, caches, exceptions/interrupts, more addressing modes), delay slots are handled for all currently-implemented delayed branches (`BRA`/`BSR`/`JSR`/`JMP`/`RTS`).
- [ ] `IpBinHeader`'s field byte offsets need validation against a real disc image (see the entry above) — currently reconstructed from cross-referenced public sources, not a single authoritative reference.
- [ ] ISO9660 filesystem parsing (needed to locate a named boot file, like IP.BIN's boot filename, within a disc image's actual file listing).
- [ ] PowerVR2 GPU core.
- [ ] AICA sound core.
- [ ] Maple bus (controllers, VMU).
- [ ] CHD *reading* (GDI, CUE/BIN, and CDI reading now exist; detection exists for all four formats).
- [ ] BIOS-free boot strategy (HLE).
- [ ] Vulkan rendering backend.
- [ ] Signed installers (.msi/.dmg/.deb) — only the unsigned app-image exists so far.
- [ ] Android packaging strategy.
- [ ] Metal backend groundwork.
- [ ] Official verbatim GPL-3.0 license text (currently a placeholder notice in `LICENSE`).

## Immediate recommended next steps

1. **Validate `IpBinHeader`'s field offsets against a real disc image** (see "Not started yet" above) — the highest-priority follow-up from this update, since the whole boot sequence depends on getting this right.
2. **Get a real `./gradlew :core-gdrom:test` run confirming the `CdiImage`/`DiscImageDetector` CDI work** — only hand-verified via an independent sandbox reconstruction so far (13/13 checks passed there), not yet run through the actual committed JUnit suite.
3. Implement ISO9660 filesystem parsing for `core-gdrom`, so the boot filename from `IpBinHeader` can actually be located and its data read — the next concrete step toward a real (BIOS-free) boot sequence. GDI, CUE/BIN, and CDI reading all exist now, so this can work against any of the three.
4. Extend disc reading to CHD — the last remaining format; will need a compressed-hunk-reading dependency (see `docs/DEPENDENCIES.md` before adding one), a step up in complexity from the other three.
5. Pivot toward PowerVR2/AICA/Maple groundwork per `docs/ROADMAP.md` Phase 1 once boot-sequence groundwork is solid.
