package org.dreamjemu.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * app-cli's first-ever JUnit test — everything else in this module is
 * I/O-heavy CLI orchestration with no dedicated test suite (see
 * /docs/STATUS.md), but {@link LoopDetector} is small, pure, PC-sequence-
 * only logic with no dependency on {@code Sh4Cpu}/{@code Bus}/disc images,
 * so it's worth testing properly rather than only via the real Sonic
 * Adventure dump that motivated it.
 */
class LoopDetectorTest {

    @Test
    void doesNotDetectANonRepeatingSequence() {
        LoopDetector detector = new LoopDetector(3, 1000);

        boolean anyDetected = false;
        for (int step = 0; step < 100; step++) {
            // Every PC is distinct — never repeats, so no period is ever even a candidate.
            anyDetected |= detector.observe(step * 2, step);
        }

        assertFalse(anyDetected);
        assertEquals(-1, detector.period(), "period should stay unset when nothing ever repeats");
    }

    @Test
    void detectsATightStableLoopAfterTheConfiguredNumberOfRepeats() {
        // Reproduces the exact shape of the real 5-instruction loop that motivated this class
        // (see LoopDetector's Javadoc): PCs 0xE2,0xE4,0xE6,0xE8,0xEA repeating forever.
        int[] loopBody = {0xE2, 0xE4, 0xE6, 0xE8, 0xEA};
        LoopDetector detector = new LoopDetector(50, 1000);

        int step = 0;
        boolean detected = false;
        for (int iteration = 0; iteration < 100 && !detected; iteration++) {
            for (int pc : loopBody) {
                detected = detector.observe(pc, step);
                step++;
                if (detected) {
                    break;
                }
            }
        }

        assertTrue(detected, "a stable 5-instruction loop should eventually be detected");
        assertEquals(5, detector.period());
        assertEquals(50, detector.consecutiveRepeats());
    }

    @Test
    void doesNotDetectBeforeTheRepeatThresholdIsReached() {
        int[] loopBody = {0x10, 0x20, 0x30};
        LoopDetector detector = new LoopDetector(1000, 10_000); // deliberately high threshold

        int step = 0;
        boolean detectedEarly = false;
        // Loop only 10 times (30 steps) — nowhere near the 1000-repeat threshold.
        for (int iteration = 0; iteration < 10; iteration++) {
            for (int pc : loopBody) {
                detectedEarly |= detector.observe(pc, step);
                step++;
            }
        }

        assertFalse(detectedEarly, "should not fire before the configured threshold is reached");
    }

    @Test
    void onlyReturnsTrueOnceForTheSameConfirmedCycle() {
        int[] loopBody = {0x100, 0x102};
        LoopDetector detector = new LoopDetector(10, 1000);

        int step = 0;
        int firstDetectionAtStep = -1;
        int detectionCount = 0;
        for (int iteration = 0; iteration < 50; iteration++) {
            for (int pc : loopBody) {
                if (detector.observe(pc, step)) {
                    detectionCount++;
                    if (firstDetectionAtStep == -1) {
                        firstDetectionAtStep = step;
                    }
                }
                step++;
            }
        }

        assertEquals(1, detectionCount, "observe() should only return true once for a single confirmed cycle");
        assertTrue(firstDetectionAtStep > 0);
    }

    @Test
    void aDifferentPeriodResetsTheRepeatCountRatherThanAccumulating() {
        // Period 2, then period 3, then period 2 again — none should reach a threshold of 5
        // just because periods happened to add up; only a genuinely CONSECUTIVE run of the
        // same period should ever count.
        LoopDetector detector = new LoopDetector(5, 1000);
        int step = 0;

        // Establish period 2 three times: PCs 1,2,1,2,1,2 (steps 0..5)
        int[] periodTwo = {1, 2, 1, 2, 1, 2};
        boolean detected = false;
        for (int pc : periodTwo) {
            detected |= detector.observe(pc, step++);
        }
        assertFalse(detected);

        // Now break the pattern with a different period before it would have reached 5 repeats.
        detected |= detector.observe(99, step++);
        detected |= detector.observe(1, step++); // period from the last '1' is now different
        assertFalse(detected, "an interrupted period should not silently accumulate toward the threshold");
    }

    @Test
    void repeatThresholdBelowOneIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LoopDetector(0, 1000));
    }

    @Test
    void resetForgetsPreviouslyObservedState() {
        int[] loopBody = {0x10, 0x20};
        LoopDetector detector = new LoopDetector(5, 1000);
        int step = 0;
        for (int i = 0; i < 5; i++) {
            for (int pc : loopBody) {
                detector.observe(pc, step++);
            }
        }
        assertTrue(detector.period() > 0, "sanity check: some period should be tracked by now");

        detector.reset();

        assertEquals(-1, detector.period(), "reset() should forget the tracked period");
        assertEquals(0, detector.consecutiveRepeats());
    }

    @Test
    void aRepeatThresholdOfOneDetectsImmediatelyOnTheFirstRepeat() {
        LoopDetector detector = new LoopDetector(1, 1000);

        assertFalse(detector.observe(0x10, 0)); // first sighting — nothing to compare yet
        assertFalse(detector.observe(0x20, 1));
        assertTrue(detector.observe(0x10, 2), "0x10 repeating at a consistent period should detect immediately with threshold 1");
    }
}
