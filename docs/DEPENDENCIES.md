# Dependencies

*Last updated: 2026-07-21.*

Every third-party dependency the project uses, why it's there, and its license. This exists mainly for two reasons: (1) GPLv3 compatibility must be checked for anything added, and (2) it's an easy place for contributors to see what's already available before reaching for something new.

## Rule for adding a new dependency

If a PR adds a new dependency (build tool plugin, library, etc.), **update this table in the same PR** (see `CONTRIBUTING.md`). Before adding anything, check:

1. **License compatibility with GPLv3.** Permissive licenses (MIT, BSD, Apache 2.0) are fine. GPLv3/LGPL-compatible copyleft licenses are fine. Do **not** add anything under a license incompatible with GPLv3 distribution (e.g. AGPL for a statically-linked/bundled dependency, or any license with a field-of-use or commercial restriction — remember this project can never be monetized, and needs to keep the door open for any future distribution model GPLv3 allows).
2. **No paid/proprietary SDKs**, ever — this project doesn't have a budget and never will.
3. **Prefer something already in this table** over adding an equivalent new dependency, where reasonable.
4. State the dependency's license explicitly in the PR description if it's not already common knowledge.

## Current dependencies

| Dependency | Version | Used in | Purpose | License | GPLv3 compatible? |
|---|---|---|---|---|---|
| [JavaFX](https://openjfx.io/) (`javafx.controls`, `javafx.fxml`, `javafx.graphics`) | 21.0.2 | `app-javafx` | Desktop UI toolkit (library/save/settings/debug UI) | GPLv2 + Classpath Exception (same terms as OpenJDK) | Yes |
| [org.openjfx.javafxplugin](https://github.com/openjfx/javafx-gradle-plugin) (Gradle plugin) | 0.1.0 | `app-javafx` (build-time only) | Wires JavaFX modules into the Gradle build | BSD 3-Clause | Yes (build tooling, not distributed with the app anyway) |
| [LWJGL](https://www.lwjgl.org/) — `lwjgl`, `lwjgl-vulkan` | 3.3.4 (via `lwjgl-bom`) | `render-vulkan` | Java bindings to the native Vulkan API — the project's only rendering backend | BSD 3-Clause | Yes |
| [JUnit Jupiter](https://junit.org/junit5/) | 5.10.2 | all modules (`testImplementation` only) | Unit testing framework | Eclipse Public License 2.0 | Yes (test-only dependency, not bundled into distributed artifacts regardless) |
| [Gradle](https://gradle.org/) | 8.7 (wrapper) | whole project (build tool) | Multi-module build system | Apache License 2.0 | Yes (build tooling, not distributed with the app) |

## Explicitly not a dependency, and never will be

- **OpenGL / any OpenGL binding.** Vulkan is the only supported graphics API — see `README.md` and `docs/ARCHITECTURE.md`. Do not add LWJGL's OpenGL module, JOGL, or any similar binding.
- **Any BIOS/firmware file or extraction tool.** The emulator must never depend on original Dreamcast console files — see `README.md`.
- **Anything that reads/writes analytics, telemetry sold to third parties, ads SDKs, or payment/licensing SDKs** — this project will never monetize, per its stated principles.

## Planned future dependencies (not yet added)

- A Vulkan-based upscaling library or a from-scratch FSR-class implementation, for `render-vulkan` (Phase 3 of `docs/ROADMAP.md`). License to be checked against the table above before adding.
- Metal bindings for macOS/arm64 (`render-metal`), once that module moves past its current abstraction-only stage.
- An Android-compatible build toolchain/dependency set once Android packaging work (`docs/ROADMAP.md` Phase 4) begins — note that plain LWJGL does not target Android, so this will need research when the time comes.
