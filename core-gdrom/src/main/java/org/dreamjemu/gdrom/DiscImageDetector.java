package org.dreamjemu.gdrom;

import java.nio.file.Path;

/**
 * Detects the format of a Dreamcast disc image file, independent of any CPU/GPU work.
 * Flagged in /docs/STATUS.md as a good, self-contained early task: it's easy to validate
 * (feed it real files of each format, assert the right enum comes back) without needing
 * the rest of the emulation core.
 */
public final class DiscImageDetector {

    private DiscImageDetector() {
    }

    /**
     * Detects the {@link DiscImageFormat} of the given file.
     * Not yet implemented.
     *
     * @param path path to a candidate disc image file
     * @return the detected format, or {@link DiscImageFormat#UNKNOWN} if it can't be determined
     */
    public static DiscImageFormat detect(Path path) {
        throw new UnsupportedOperationException(
                "Disc image format detection not implemented yet — see docs/ROADMAP.md Phase 1."
        );
    }
}
