# Guidance for AI Contributors and AI/Human Reviewers

This project explicitly supports AI-assisted and AI-authored contributions. This document gives concrete instructions for (a) an AI agent producing a contribution, and (b) anyone — human or AI — reviewing a pull request.

## If you are an AI producing a contribution

1. **Read first, in this order:** `README.md`, `docs/STATUS.md`, `docs/ROADMAP.md`, `docs/ARCHITECTURE.md`, `CONTRIBUTING.md`. Do not propose changes without reading the current status/roadmap — duplicated or already-rejected work wastes reviewer time.
2. **Pick a task from `docs/ROADMAP.md`'s "Next steps"**, or a flaw described in `docs/STATUS.md`, unless the user/operator directing you has a specific, in-scope task in mind.
3. **Never fabricate test results.** If you cannot actually execute/build/run the emulator or the specific test case, say so plainly in the PR instead of asserting it was tested. A false "tested against Sonic Adventure, works correctly" claim is worse than admitting no test was run.
4. **Respect hard project boundaries** even if a task seems to shortcut them: no BIOS/firmware dependencies, no OpenGL, no piracy-adjacent content, no monetization-related code (ads, telemetry sold to third parties, paywalls, etc.).
5. **Write the PR description using the required four-part structure** from `CONTRIBUTING.md` (what / how / why / tested-with), in English, and **explicitly state AI usage — "yes" or "no", not just when AI was used.** Since you are (by definition, in this section) an AI producing the contribution, this will be "yes"; say briefly what you did versus what a human reviewed/tested.
6. **Update `docs/STATUS.md`, `docs/ROADMAP.md`, and `CHANGELOG.md`** in the same PR if your change materially changes what's implemented or what should be worked on next — including `docs/STATUS.md`'s "Last updated" date and a dated `CHANGELOG.md` entry (with its own AI usage note).
7. **Keep the change scoped.** Prefer several small, reviewable PRs over one large one.
8. **If you add a new dependency, update `docs/DEPENDENCIES.md`** in the same PR, including a GPLv3 license-compatibility check.

## If you are reviewing a pull request (AI or human)

Use this checklist:

- [ ] Is the PR description in English, and does it clearly answer what / how / why / tested-with?
- [ ] Is the "tested with" step credible and specific (named game/test-case, observed before/after behavior), not vague or generic?
- [ ] **Does the PR explicitly state AI usage (yes or no), not leave it unstated?** A PR that used AI help but doesn't disclose it is a rules violation, not just a gap.
- [ ] Does the change avoid introducing any dependency on a BIOS dump or other original-console file?
- [ ] Does the change avoid OpenGL, and stay consistent with the Vulkan-first (and, where relevant, Metal-abstraction) rendering architecture in `docs/ARCHITECTURE.md`?
- [ ] Does the change respect existing module boundaries, or justify a new module?
- [ ] Does it avoid piracy-adjacent content (ROM/ISO links, copyrighted asset bundling)?
- [ ] Does it avoid introducing any monetization mechanism (ads, paywalls, telemetry sale, etc.) — this project will never seek to make money?
- [ ] If the change affects current status or next steps, are `docs/STATUS.md` (with an updated date) / `docs/ROADMAP.md` / `CHANGELOG.md` updated?
- [ ] If a new dependency was added, is `docs/DEPENDENCIES.md` updated with a license-compatibility check?
- [ ] Are commits reasonably focused and does the diff match the stated intent?

If any required item is missing, ask the contributor to add it rather than merging with gaps — or close the PR per `CONTRIBUTING.md` if the contributor is unresponsive or the PR is fundamentally out of scope.

AI reviewers should flag (not silently "fix") anything ambiguous — e.g., a plausible-looking but unverifiable performance claim — so a human maintainer can make the final call.
