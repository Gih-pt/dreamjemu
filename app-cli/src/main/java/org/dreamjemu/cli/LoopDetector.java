package org.dreamjemu.cli;

import java.util.HashMap;
import java.util.Map;

/**
 * Detects a stable, repeating cycle in a sequence of program-counter values
 * — built specifically because a real Sonic Adventure boot run reached this
 * project's entire 5,000,000-step budget without ever hitting an
 * unimplemented instruction, and turned out (per a {@code --log-level
 * TRACE} run) to be a tight 5-instruction byte-at-a-time memory-fill loop
 * (almost certainly a {@code crt0}-style {@code .bss}-clearing loop) —
 * legitimate, finite work, just far too slow to finish within any
 * reasonable step budget, and not distinguishable from a genuine infinite
 * spin-wait on unmodeled hardware state without actually detecting the
 * repetition. See {@code Main.attemptMinimalBoot} for how this is used:
 * once a loop is confirmed, stepping stops early with a diagnostic summary
 * instead of silently exhausting the step budget (and, if tracing, filling
 * a log file with millions of identical lines).
 *
 * <p><b>Algorithm:</b> for each PC observed, remember the step index it was
 * last seen at. When a PC recurs, the distance since its last occurrence is
 * a candidate period. If that same period recurs {@code repeatThreshold}
 * times in a row, the cycle is declared stable — chosen deliberately high
 * enough (see {@code Main}'s usage) that only a genuinely, consistently
 * repeating cycle can trigger it; a call/return pair or any other
 * coincidental single revisit of an address cannot.
 *
 * <p>This is deliberately a small, self-contained, PC-sequence-only utility
 * — it knows nothing about {@code Sh4Cpu}, {@code Bus}, or opcodes, so it's
 * easy to test in isolation (see {@code LoopDetectorTest}) independent of
 * the rest of {@code app-cli}, which otherwise has no dedicated test suite
 * (it's I/O-heavy orchestration code — see /docs/STATUS.md).
 */
public final class LoopDetector {

    private final int repeatThreshold;
    private final int maxTrackedPcs;
    private final Map<Integer, Long> lastSeenAtStep = new HashMap<>();

    private long candidatePeriod = -1;
    private int repeatsOfCandidatePeriod = 0;

    /**
     * @param repeatThreshold how many times the SAME period must recur, back to back,
     *                        before a cycle is declared stable — must be at least 1
     * @param maxTrackedPcs   caps how many distinct PCs are remembered, bounding memory use;
     *                        once exceeded, newly-seen addresses stop being recorded (existing
     *                        ones keep updating), which in practice only matters for code that
     *                        visits an enormous number of distinct addresses without ever
     *                        looping tightly — exactly the case where loop detection isn't
     *                        useful anyway
     */
    public LoopDetector(int repeatThreshold, int maxTrackedPcs) {
        if (repeatThreshold < 1) {
            throw new IllegalArgumentException("repeatThreshold must be at least 1, was " + repeatThreshold);
        }
        this.repeatThreshold = repeatThreshold;
        this.maxTrackedPcs = maxTrackedPcs;
    }

    /**
     * Call once per instruction, with the PC about to be executed and a monotonically
     * increasing step index (e.g. a simple counter starting at 0).
     *
     * @return true the moment a stable cycle is confirmed (only ever returned once per
     *         confirmed cycle — call {@link #reset()} to detect a further/different cycle
     *         afterward, e.g. if the caller decides to keep stepping past this one)
     */
    public boolean observe(int pc, long stepIndex) {
        Long lastStep = lastSeenAtStep.get(pc);
        boolean justConfirmed = false;

        if (lastStep != null) {
            long period = stepIndex - lastStep;
            if (period == candidatePeriod) {
                repeatsOfCandidatePeriod++;
            } else {
                candidatePeriod = period;
                repeatsOfCandidatePeriod = 1;
            }
            // Checked here rather than only inside the `if` branch above, so a
            // repeatThreshold of 1 (detect on the very first repeat) works correctly —
            // otherwise establishing a brand-new candidate period would never itself
            // count as satisfying a threshold of exactly 1.
            if (repeatsOfCandidatePeriod == repeatThreshold) {
                justConfirmed = true;
            }
        }

        if (lastStep != null || lastSeenAtStep.size() < maxTrackedPcs) {
            lastSeenAtStep.put(pc, stepIndex);
        }

        return justConfirmed;
    }

    /** The confirmed (or currently-candidate) period, in instructions. -1 if no PC has ever repeated yet. */
    public long period() {
        return candidatePeriod;
    }

    /** How many consecutive times {@link #period()} has recurred so far. */
    public int consecutiveRepeats() {
        return repeatsOfCandidatePeriod;
    }

    /** Forgets everything observed so far, as if newly constructed — e.g. to look for a different cycle after this one. */
    public void reset() {
        lastSeenAtStep.clear();
        candidatePeriod = -1;
        repeatsOfCandidatePeriod = 0;
    }
}
