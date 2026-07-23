# Changelog

All notable changes to this project should be documented here. Format loosely follows [Keep a Changelog](https://keepachangelog.com/).

*Note: entries up to and including `9df24ab` were reconstructed retroactively from actual commit hashes and GitHub Actions run timestamps, since not every early commit had a matching changelog entry at the time. Going forward, per `CONTRIBUTING.md`, every impactful PR must add its own dated entry here directly — no more retroactive reconstruction should be needed.*

## [Unreleased]

### Added — 2026-07-23
- `core-cpu-sh4`: SH-4 interpreter bring-up. Implemented `Sh4Cpu` (register file R0-R15, PC, PR, a T-flag-only status register) with a `step()` fetch-decode-execute loop, working against the generic `Bus` interface from `core-system` (not coupled to `SystemBus`/`DreamcastAddressMap`, so the interpreter can be tested and eventually reused independent of the Dreamcast-specific memory map). Implements 12 instructions: `NOP`, `MOV #imm,Rn`, `MOV Rm,Rn`, `ADD #imm,Rn`, `ADD Rm,Rn`, `SUB Rm,Rn`, `CMP/EQ Rm,Rn`, `CMP/EQ #imm,R0`, `BT`, `BF`, `BRA`, `MOV.L Rm,@Rn`/`MOV.L @Rm,Rn`. Unimplemented opcodes throw `UnsupportedOperationException` with the opcode and PC, by design. **Known limitation, tracked as follow-up work: delay slots are not implemented** (BRA branches immediately instead of executing the following instruction first, unlike real SH-4 hardware) — this must be fixed before real code can run correctly. 13 JUnit tests added, covering every implemented instruction plus an integration test: a hand-assembled loop program summing 5+4+3+2+1 via real conditional branching, storing the result to memory, and reading it back (25 CPU steps, verified against both the register and memory state).
- *AI assistance: yes — implemented with Claude (Anthropic), tested locally with a JDK-based manual verification harness in a sandbox (no Gradle/Maven Central access there); needs confirmation via `./gradlew :core-cpu-sh4:test` on a real machine.*

### Fixed — 2026-07-22 (commit `d0464cf`)
- `app-javafx`: fixed "Error: JavaFX runtime components are missing, and are required to run this application" when launching the `jpackage` app-image. Cause: the Java launcher refuses to start a packaged app whose main class directly extends `javafx.application.Application` outside the module path. Fix: added a `Launcher` class that doesn't extend `Application` and just forwards to `Main`; pointed `application.mainClass` (Gradle) and `--main-class` (jpackage) at `Launcher` instead of `Main` directly.
- **Confirmed working end-to-end on Linux**: the packaged native binary (`app-javafx/build/jpackage/DreamJEmu/bin/DreamJEmu`) launches standalone (no Gradle, no manually-set `JAVA_HOME`) and correctly shows the bootstrap window.
- *AI assistance: yes — diagnosed and fixed with Claude (Anthropic) from the error message; confirmed working by the project owner on their machine, including a screenshot of the packaged app running.*

### Added — 2026-07-22
- `app-javafx`: added a `jpackageImage` Gradle task that builds a native, self-contained application image (bundled Java runtime, platform-native launcher — `DreamJEmu.exe` / `DreamJEmu.app` / `DreamJEmu`) via `jpackage`, so end users won't need Java installed separately. Produces an app-image (a runnable folder), not yet a signed installer (.msi/.dmg/.deb) — that needs additional platform tooling and is a follow-up step. Wired into `.github/workflows/nightly.yml` and `.github/workflows/release.yml`, replacing their previous `TODO: jpackage...` placeholder steps.
- *AI assistance: yes — implemented with Claude (Anthropic). Initially only smoke-tested in a sandbox (no Maven Central access there to run the full Gradle task); confirmed working against the real project shortly after, once the Launcher fix above was applied (see that entry).*

### Added — 2026-07-22 (commit `9df24ab`)
- `core-gdrom`: implemented `GdiTrackType`, `GdiTrack`, and `GdiImage` — parses a `.gdi` file's track list and reads sector data from the referenced track files (resolved relative to the `.gdi`'s directory), correctly locating the right track/file/byte-offset for a given LBA across multiple tracks. 7 JUnit tests added, covering multi-track parsing, sector reads from two different tracks, an out-of-range LBA, a mismatched track-count header, and a missing referenced track file.
- *AI assistance: yes — implemented with Claude (Anthropic), tested locally by the project owner (`./gradlew :core-gdrom:test`, all 15 tests passing — 8 from format detection + 7 new).*

### Fixed — 2026-07-21 (commit `819b3dc`)
- `.github/workflows/nightly.yml` and `.github/workflows/release.yml`: fixed an invalid-YAML bug where a `run:` step's plain-scalar value contained an unquoted `: ` (colon-space) sequence (`echo "TODO: ..."`), which GitHub Actions rejects as invalid workflow syntax. Switched those steps to block-scalar (`run: |`) style. Verified with `python3`'s `yaml.safe_load` before committing, and confirmed on GitHub Actions afterward that both workflow files pass validation.
- *AI assistance: yes — diagnosed and fixed with Claude (Anthropic) from a screenshot of the GitHub Actions error; confirmed working by the project owner on GitHub Actions.*

### Added — 2026-07-21 (commit `aa0f117`)
- `core-gdrom`: implemented `DiscImageDetector`, identifying GDI, CDI, CHD, and CUE/BIN Dreamcast disc image formats via file extension plus structural content verification (magic bytes / text structure), with content-based fallback when the extension is missing or wrong. 8 JUnit tests added.
- Added `docs/DEPENDENCIES.md`: every third-party dependency, its purpose, and a GPLv3 license-compatibility check, plus rules for adding new ones.
- `CONTRIBUTING.md`, `docs/AI_CONTRIBUTIONS.md`, `.github/PULL_REQUEST_TEMPLATE.md`, `docs/AI_CONTINUATION.md`: now require mandatory AI-usage disclosure ("yes" or "no", not only when AI was used) on every PR, and require keeping `docs/STATUS.md` / `docs/ROADMAP.md` / `CHANGELOG.md` current (with accurate dates) on every impactful contribution.
- *AI assistance: yes — implemented with Claude (Anthropic), tested locally by the project owner (`./gradlew :core-gdrom:test`, all tests passing).*

### Added — 2026-07-21 (commit `d462ffb`)
- `core-system`: implemented the memory map and system bus (`Bus`/`MemoryRegion` interfaces, `RamRegion`, `UnmappedRegion`, `DreamcastAddressMap`, `SystemBus`). Handles SH-4 cache-area address mirroring (P0-P3) and the four physical mirrors of main RAM; reserves VRAM/AICA RAM ranges as placeholders for `core-gpu-pvr2`/`core-aica`. 7 JUnit tests added, covering read/write round-trips, little-endian ordering, RAM mirroring, cache-area masking, and unmapped-region fallback.
- *AI assistance: yes — implemented with Claude (Anthropic), tested locally by the project owner (`./gradlew :core-system:test`, all tests passing).*

### Changed — 2026-07-21 (commit `6ca3ef`)
- `docs/STATUS.md` updated to record that the Gradle multi-module build and the placeholder JavaFX shell (`app-javafx`) were verified working end-to-end on a real machine (Linux, Java 21, Gradle 8.7) — the bootstrap window launches and displays correctly.
- *AI assistance: yes — the update text was drafted with Claude (Anthropic); the underlying verification (build + running the JavaFX window) was performed by the project owner.*

### Added — 2026-07-21 (commit `3be1306`)
- Added a conservative `gradle.properties` (reduced JVM heap `-Xmx768m`, no persistent daemon, no parallel builds) so the project builds reliably on modest/low-RAM hardware (~4GB machines) — added after a build attempt caused system instability on the project owner's machine with only ~1.3GB of available memory.
- *AI assistance: yes — drafted with Claude (Anthropic) after diagnosing the low-memory situation together with the project owner; applied and confirmed working locally by the project owner.*

### Added — 2026-07-21 (commit `c0b3ebc`)
- Generated and committed the Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`), required for the CI workflows (which call `./gradlew build`) and for anyone cloning the repository to build without a separately installed Gradle.
- *AI assistance: no direct code generation (the wrapper is machine-generated by `gradle wrapper --gradle-version 8.7`) — the command itself was suggested by Claude (Anthropic) and run locally by the project owner.*

### Added — 2026-07-21 (commit `07bc938`, initial bootstrap)
- Initial project bootstrap: repository structure, documentation (`README.md`, `CONTRIBUTING.md`, `docs/STATUS.md`, `docs/ROADMAP.md`, `docs/ARCHITECTURE.md`, `docs/AI_CONTRIBUTIONS.md`, `docs/AI_CONTINUATION.md`, `docs/MINIMUM_REQUIREMENTS.md`, Portuguese `docs/pt/README.pt.md`), GitHub issue/PR templates, CI workflow skeletons (build/nightly/release), GPLv3 license notice, and a minimal Gradle multi-module skeleton with a placeholder JavaFX window.
- *AI assistance: yes — the entire initial project structure and documentation was bootstrapped with extensive help from Claude (Anthropic), per the project's stated openness to AI-assisted contribution (see `docs/AI_CONTRIBUTIONS.md`).*
