package org.dreamjemu.system;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Placeholder region for address ranges that are reserved in the memory map
 * but not backed by a real implementation yet (VRAM, AICA sound RAM/registers,
 * Maple/GD-ROM registers, etc. — see /docs/ROADMAP.md for which modules will
 * eventually own these).
 *
 * Reads return 0 and writes are ignored, both logged at FINE level so this
 * is easy to spot during early bring-up/debugging without spamming normal
 * output. This intentionally does NOT throw, because a real boot sequence
 * (even an HLE one) is expected to touch several not-yet-implemented
 * peripheral registers before every core module exists — throwing here
 * would make incremental bring-up impractical.
 */
public final class UnmappedRegion implements MemoryRegion {

    private static final Logger LOG = Logger.getLogger(UnmappedRegion.class.getName());

    private final String name;
    private final long size;

    public UnmappedRegion(String name, long size) {
        this.name = name;
        this.size = size;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public long size() {
        return size;
    }

    private void logAccess(String kind, long offset) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(() -> String.format("%s: unimplemented %s at offset 0x%X", name, kind, offset));
        }
    }

    @Override
    public byte read8(long offset) {
        logAccess("read8", offset);
        return 0;
    }

    @Override
    public short read16(long offset) {
        logAccess("read16", offset);
        return 0;
    }

    @Override
    public int read32(long offset) {
        logAccess("read32", offset);
        return 0;
    }

    @Override
    public long read64(long offset) {
        logAccess("read64", offset);
        return 0;
    }

    @Override
    public void write8(long offset, byte value) {
        logAccess("write8", offset);
    }

    @Override
    public void write16(long offset, short value) {
        logAccess("write16", offset);
    }

    @Override
    public void write32(long offset, int value) {
        logAccess("write32", offset);
    }

    @Override
    public void write64(long offset, long value) {
        logAccess("write64", offset);
    }
}
