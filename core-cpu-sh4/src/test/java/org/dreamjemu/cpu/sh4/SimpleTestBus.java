package org.dreamjemu.cpu.sh4;

import org.dreamjemu.system.Bus;

/**
 * A trivial, flat byte-array-backed {@link Bus} used only by Sh4Cpu's own
 * tests. Deliberately has no relationship to core-system's Dreamcast-specific
 * memory map (SystemBus/DreamcastAddressMap) — the interpreter only depends
 * on the generic Bus interface (see Bus's own Javadoc), so its tests should
 * be able to run against the simplest possible implementation.
 */
final class SimpleTestBus implements Bus {

    private final byte[] memory;

    SimpleTestBus(int sizeBytes) {
        this.memory = new byte[sizeBytes];
    }

    private int offset(long address) {
        return (int) address;
    }

    @Override
    public byte read8(long address) {
        return memory[offset(address)];
    }

    @Override
    public short read16(long address) {
        int o = offset(address);
        return (short) ((memory[o] & 0xFF) | ((memory[o + 1] & 0xFF) << 8));
    }

    @Override
    public int read32(long address) {
        int o = offset(address);
        return (memory[o] & 0xFF)
                | ((memory[o + 1] & 0xFF) << 8)
                | ((memory[o + 2] & 0xFF) << 16)
                | ((memory[o + 3] & 0xFF) << 24);
    }

    @Override
    public long read64(long address) {
        long low = read32(address) & 0xFFFFFFFFL;
        long high = read32(address + 4) & 0xFFFFFFFFL;
        return low | (high << 32);
    }

    @Override
    public void write8(long address, byte value) {
        memory[offset(address)] = value;
    }

    @Override
    public void write16(long address, short value) {
        int o = offset(address);
        memory[o] = (byte) (value & 0xFF);
        memory[o + 1] = (byte) ((value >> 8) & 0xFF);
    }

    @Override
    public void write32(long address, int value) {
        int o = offset(address);
        memory[o] = (byte) (value & 0xFF);
        memory[o + 1] = (byte) ((value >> 8) & 0xFF);
        memory[o + 2] = (byte) ((value >> 16) & 0xFF);
        memory[o + 3] = (byte) ((value >> 24) & 0xFF);
    }

    @Override
    public void write64(long address, long value) {
        write32(address, (int) (value & 0xFFFFFFFFL));
        write32(address + 4, (int) ((value >>> 32) & 0xFFFFFFFFL));
    }

    /** Writes a 16-bit instruction word at the given address, for building hand-assembled test programs. */
    void writeInstruction(int address, int opcode) {
        write16(address, (short) opcode);
    }
}
