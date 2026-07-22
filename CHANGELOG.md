# Changelog

All notable changes to this project should be documented here. Format loosely follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Added — 2026-07-22
- `core-gdrom`: implemented `GdiTrackType`, `GdiTrack`, and `GdiImage` — parses a `.gdi` file's track list and reads sector data from the referenced track files (resolved relative to the `.gdi`'s directory), correctly locating the right track/file/byte-offset for a given LBA across multiple tracks. 7 JUnit tests added, covering multi-track parsing, sector reads from two different tracks, an out-of-range LBA, a mismatched track-count header, and a missing referenced track file.
- *AI assistance: yes — implemented with Claude (Anthropic), tested locally by the project owner (`./gradlew :core-gdrom:test`, all tests passing).*

### Fixed — 2026-07-21
- `.github/workflows/nightly.yml` and `.github/workflows/release.yml`: fixed an invalid-YAML bug where a `run:` step's plain-scalar value contained an unquoted `: ` (colon-space) sequence (`echo "TODO: ..."`), which GitHub Actions rejects as invalid workflow syntax. Switched those steps to block-scalar (`run: |`) style, matching the pattern already used elsewhere in the same files. Verified with `python3`'s `yaml.safe_load` before committing, and confirmed on GitHub Actions afterward that both workflow files pass validation.
- *AI assistance: yes — diagnosed and fixed with Claude (Anthropic) from a screenshot of the GitHub Actions error; validated locally and confirmed working by the project owner on GitHub Actions.*

### Added — 2026-07-21
- `core-gdrom`: implemented `DiscImageDetector`, identifying GDI, CDI, CHD, and CUE/BIN Dreamcast disc image formats via file extension plus structural content verification (magic bytes / text structure), with content-based fallback when the extension is missing or wrong. 8 JUnit tests added, covering all four formats, a mismatched-extension case, garbage content, an unrelated file, and a non-existent file. Does not require or read any original console/BIOS file.
- *AI assistance: yes — implemented with Claude (Anthropic), reviewed and tested locally by the project owner (`./gradlew :core-gdrom:test`, all tests passing).*

### Added — 2026-07-20
- `core-system`: implemented the memory map and system bus (`Bus`/`MemoryRegion` interfaces, `RamRegion`, `UnmappedRegion`, `DreamcastAddressMap`, `SystemBus`). Handles SH-4 cache-area address mirroring (P0–P3) and the four physical mirrors of main RAM; reserves VRAM/AICA RAM ranges as placeholders for `core-gpu-pvr2`/`core-aica`. 7 JUnit tests added, covering read/write round-trips, little-endian ordering, RAM mirroring, cache-area masking, and unmapped-region fallback.
- *AI assistance: yes — implemented with Claude (Anthropic), reviewed and tested locally by the project owner (`./gradlew :core-system:test`, all tests passing).*
- Verified the Gradle multi-module build and the placeholder JavaFX shell (`app-javafx`) launch correctly end-to-end (Linux, Java 21, Gradle 8.7).
- Added conservative `gradle.properties` (reduced JVM heap, no daemon, no parallel builds) so the project builds reliably on modest/low-RAM hardware.

### Added — initial bootstrap
- Initial project bootstrap: repository structure, documentation (`README.md`, `CONTRIBUTING.md`, `docs/STATUS.md`, `docs/ROADMAP.md`, `docs/ARCHITECTURE.md`, `docs/AI_CONTRIBUTIONS.md`, `docs/AI_CONTINUATION.md`, `docs/MINIMUM_REQUIREMENTS.md`, Portuguese `docs/pt/README.pt.md`), GitHub issue/PR templates, CI workflow skeletons (build/nightly/release), GPLv3 license notice, and a minimal Gradle multi-module skeleton with a placeholder JavaFX window.
- *AI assistance: yes — the entire initial project structure and documentation was bootstrapped with extensive help from Claude (Anthropic), per the project's stated openness to AI-assisted contribution (see `docs/AI_CONTRIBUTIONS.md`).*
