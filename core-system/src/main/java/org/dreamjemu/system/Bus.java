package org.dreamjemu.system;

/**
 * The system bus contract used by the CPU core (and, later, DMA-capable
 * peripherals) to read and write memory-mapped addresses.
 *
 * All accesses are little-endian, matching the SH-4's default byte order
 * in the modes the Dreamcast uses.
 *
 * This interface intentionally knows nothing about which physical device
 * (RAM, VRAM, a register block, etc.) backs a given address — that routing
 * is {@link SystemBus}'s job. Keeping this as a narrow interface means
 * core-cpu-sh4 can be unit-tested against a trivial in-memory Bus
 * implementation without depending on the rest of the system.
 */
public interface Bus {

    byte read8(long address);

    short read16(long address);

    int read32(long address);

    long read64(long address);

    void write8(long address, byte value);

    void write16(long address, short value);

    void write32(long address, int value);

    void write64(long address, long value);
}
