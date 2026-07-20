package org.dreamjemu.cpu.sh4;

/**
 * Placeholder for the Hitachi/Renesas SH-4 CPU core (Dreamcast's main CPU).
 *
 * Per /docs/ROADMAP.md Phase 1, this should start as a straightforward interpreter,
 * validated instruction-by-instruction against a known-good test suite, before any
 * performance work (e.g. a JIT/dynarec) is considered. Accuracy first.
 *
 * This must never depend on a BIOS/firmware dump — boot behavior is handled via
 * HLE (High-Level Emulation) in core-system, not by executing a real boot ROM here.
 */
public class Sh4Cpu {

    // TODO: registers, MMU, cache, instruction decode/dispatch, exception handling.

    public Sh4Cpu() {
        // Intentionally empty during bootstrap — see docs/STATUS.md for what's next.
    }

    /**
     * Executes a single instruction cycle. Not yet implemented.
     */
    public void step() {
        throw new UnsupportedOperationException(
                "SH-4 interpreter not implemented yet — see docs/ROADMAP.md Phase 1."
        );
    }
}
