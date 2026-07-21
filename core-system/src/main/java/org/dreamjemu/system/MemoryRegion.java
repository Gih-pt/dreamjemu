package org.dreamjemu.system;

/**
 * A single backing device for a range of the physical address space —
 * for example, main system RAM, video RAM, or a peripheral's register block.
 *
 * Implementations receive an <b>offset relative to the start of their own
 * range</b>, not a raw system address; {@link SystemBus} is responsible for
 * translating physical addresses (including cache-mirror handling) into a
 * (region, offset) pair before calling into a region.
 *
 * This is the extension point other modules (core-gpu-pvr2 for VRAM,
 * core-aica for sound RAM/registers, core-maple, core-gdrom) are expected to
 * implement against once they exist — see /docs/ROADMAP.md.
 */
public interface MemoryRegion {

    /** Human-readable name, used in logging/diagnostics (e.g. "Main RAM", "VRAM"). */
    String name();

    /** Size of this region in bytes. */
    long size();

    byte read8(long offset);

    short read16(long offset);

    int read32(long offset);

    long read64(long offset);

    void write8(long offset, byte value);

    void write16(long offset, short value);

    void write32(long offset, int value);

    void write64(long offset, long value);
}
