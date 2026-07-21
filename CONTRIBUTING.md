# Contributing to DreamJEmu

Thank you for considering a contribution — human or AI-assisted. This project explicitly welcomes both, on equal footing, as long as every contribution follows the rules below.

## Language

**All contributions (code, comments, commit messages, issues, and pull requests) must be written in English.** The project's primary documentation language is English, with Portuguese as the secondary language (see `docs/pt/`). The emulator's UI is designed to support additional languages in the future via its i18n resource system, but project *contribution* communication stays in English so anyone can review anything.

Pull requests and issues not written in English will be closed.

## What every pull request must include

Every PR description must clearly answer all four of these questions. **PRs missing any of them will be closed without merging**, regardless of code quality:

1. **What does this change do?** A clear, specific description of the change.
2. **How does it do it?** A short technical explanation of the approach/implementation.
3. **Why is it necessary?** What problem it solves, what accuracy/compatibility/performance gap it closes, or what part of the roadmap it advances.
4. **What was tested, and how?** Which game(s) or test case(s) were used to validate the change, what the behavior was before, and what it is after. For non-game-specific changes (tooling, build system, UI), describe the test performed instead (e.g. "built and ran on Windows 11 and Ubuntu 24.04, verified the settings screen opens and saves correctly").

A PR without a real test/validation step (even "no regressions observed running the SH-4 test ROM suite" or similar) is not acceptable — untested claims of correctness are not useful for an accuracy-focused project.

## AI usage disclosure is mandatory on every PR

**Every pull request must state, explicitly, whether AI assistance was used or not** — not just when it was. A one-line statement is enough:

- `AI usage: yes — <brief note on what was AI-assisted, e.g. "implementation drafted with Claude, tested locally by me">`, or
- `AI usage: no — written entirely by hand`

This applies regardless of how much or how little AI was involved (full generation, partial help, code review assistance, etc.) — partial use still counts as "yes". This is not a judgment of quality either way; it's a transparency requirement so reviewers and future contributors always know the provenance of a change. PRs missing this line will be treated as missing required information, same as the four items above.

## Where to focus

Contributions are **preferentially expected to target areas flagged as needing work** in [`docs/STATUS.md`](docs/STATUS.md) and [`docs/ROADMAP.md`](docs/ROADMAP.md). If you want to work on something not listed there, please open an issue first to discuss it — this avoids wasted effort on things that are out of scope (e.g. OpenGL support, which will never be added) or already being worked on elsewhere.

## Keep project documentation current — every impactful contribution, not just occasionally

`docs/STATUS.md`, `docs/ROADMAP.md`, and `CHANGELOG.md` must be kept up to date **as part of the same PR**, whenever a contribution meaningfully changes what's implemented, what the recommended next steps are, or is otherwise worth recording. In practice, for anything beyond a trivial fix:

- Update `docs/STATUS.md`'s "Done so far" / "Not started yet" / "Immediate recommended next steps" sections, and its **"Last updated" date**, to reflect the change.
- Add a dated entry to `CHANGELOG.md` describing what changed, including the AI usage disclosure for that entry (see above).
- If the change affects priorities or what should be worked on next, update `docs/ROADMAP.md` too.
- If the change adds, removes, or upgrades a dependency, update `docs/DEPENDENCIES.md` too.

Always use the actual current date for these updates — don't leave a stale date from a previous contribution. Keeping these documents current is what lets a new contributor (human or AI, including in a fresh chat via `docs/AI_CONTINUATION.md`) trust them as the source of truth without having to dig through commit history.

## Rules that apply regardless of who (or what) is contributing

- No code that requires a BIOS dump, flash ROM dump, or any other file extracted from real Dreamcast hardware. Everything must work via clean-room / HLE-style emulation.
- No code, links, or references that facilitate piracy (ROM/ISO sources, copyrighted asset bundling, etc.).
- No OpenGL code or dependencies. Vulkan is the only supported graphics API; Metal (macOS/arm64) is the only other backend planned, and only its initial abstraction layer is an early-stage goal.
- Follow the existing module boundaries described in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md). If your change needs a new module, explain why in the PR.
- Keep commits focused; avoid bundling unrelated changes in one PR.
- Add or update unit/hardware-behavior tests where practical, especially for CPU (SH-4), GPU (PowerVR2), and AICA sound-core logic, where cycle/behavior accuracy matters most.
- **Adding a new third-party dependency requires updating [`docs/DEPENDENCIES.md`](docs/DEPENDENCIES.md) in the same PR**, including a license compatibility check against GPLv3 (see that document for the exact rule). No paid/proprietary SDKs, ever.

## AI-assisted contributions

AI-assisted and fully AI-generated contributions are explicitly welcome. They must follow **all** of the rules above exactly like human contributions — there is no separate, lower bar. In addition:

- Disclose that AI assistance was used in the PR description (which model/tool, briefly).
- The four required PR answers (what/how/why/tested-with) must reflect an actual validation step, not a generated claim. If the AI (or its operator) could not actually run/test the change, say so explicitly rather than asserting it was tested.
- See [`docs/AI_CONTRIBUTIONS.md`](docs/AI_CONTRIBUTIONS.md) for more detailed guidance aimed specifically at AI agents and at anyone (human or AI) reviewing PRs.

## Non-conforming contributions

Any issue or pull request that does not follow the rules above — wrong language, missing required information, out-of-scope (OpenGL, BIOS-dependent features, piracy-adjacent content), or unsupported-hardware complaints — **will be closed**. This isn't personal; it keeps the project's history usable and its scope coherent for a hobby project maintained in spare time.

## Release channels

- **Nightly**: built automatically from the latest commits on the main branch. Expect breakage; this channel exists for testing and rapid contributor feedback.
- **Stable**: more polished, tested releases, cut periodically once nightly builds have proven reasonably solid. See [`.github/workflows`](.github/workflows) for how these are built.

## Code of Conduct

Be respectful. Disagreements about code and architecture are fine and expected; personal attacks are not. See [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md).
