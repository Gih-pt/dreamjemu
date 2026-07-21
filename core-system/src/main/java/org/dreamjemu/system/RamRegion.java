package org.dreamjemu.system;

import java.util.Objects;

/**
 * A simple, fully-implemented {@link MemoryRegion} backed by a flat byte
 * array — used for main system RAM. All multi-byte accesses are
 * little-endian, matching the SH-4.
 *
 * Unaligned accesses are permitted (real hardware and most game code assume
 * they either don't happen or are handled by the CPU core raising an
 * alignment exception at a higher layer — this class just does the byte
 * shuffling and lets the caller worry about correctness of alignment).
 */
public final class RamRegion implements MemoryRegion {

    private final String name;
    private final byte[] data;

    public RamRegion(String name, int sizeBytes) {
        this.name = Objects.requireNonNull(name, "name");
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("sizeBytes must be positive, got " + sizeBytes);
        }
        this.data = new byte[sizeBytes];
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public long size() {
        return data.length;
    }

    private int off(long offset) {
        if (offset < 0 || offset >= data.length) {
            throw new IndexOutOfBoundsException(
                    name + ": offset 0x" + Long.toHexString(offset) +
                            " out of bounds for size 0x" + Long.toHexString(data.length));
        }
        return (int) offset;
    }

    @Override
    public byte read8(long offset) {
        return data[off(offset)];
    }

    @Override
    public short read16(long offset) {
        int o = off(offset);
        return (short) ((data[o] & 0xFF) | ((data[o + 1] & 0xFF) << 8));
    }

    @Override
    public int read32(long offset) {
        int o = off(offset);
        return (data[o] & 0xFF)
                | ((data[o + 1] & 0xFF) << 8)
                | ((data[o + 2] & 0xFF) << 16)
                | ((data[o + 3] & 0xFF) << 24);
    }

    @Override
    public long read64(long offset) {
        long low = read32(offset) & 0xFFFFFFFFL;
        long high = read32(offset + 4) & 0xFFFFFFFFL;
        return low | (high << 32);
    }

    @Override
    public void write8(long offset, byte value) {
        data[off(offset)] = value;
    }

    @Override
    public void write16(long offset, short value) {
        int o = off(offset);
        data[o] = (byte) (value & 0xFF);
        data[o + 1] = (byte) ((value >> 8) & 0xFF);
    }

    @Override
    public void write32(long offset, int value) {
        int o = off(offset);
        data[o] = (byte) (value & 0xFF);
        data[o + 1] = (byte) ((value >> 8) & 0xFF);
        data[o + 2] = (byte) ((value >> 16) & 0xFF);
        data[o + 3] = (byte) ((value >> 24) & 0xFF);
    }

    @Override
    public void write64(long offset, long value) {
        write32(offset, (int) (value & 0xFFFFFFFFL));
        write32(offset + 4, (int) ((value >>> 32) & 0xFFFFFFFFL));
    }
}
