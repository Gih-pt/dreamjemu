package org.dreamjemu.cpu.sh4;

import org.dreamjemu.system.Bus;

/**
 * SH-4 CPU interpreter core (Dreamcast's main CPU).
 *
 * Per /docs/ROADMAP.md Phase 1, this starts as a small, testable instruction
 * subset rather than full ISA coverage, validated with hand-assembled test
 * programs (see Sh4CpuTest) rather than real game code at this stage.
 * Performance work (a JIT/dynarec) is explicitly out of scope until this
 * interpreter is accurate — see docs/ROADMAP.md.
 *
 * This must never depend on a BIOS/firmware dump — boot behavior will be
 * handled via HLE (High-Level Emulation) elsewhere, not by executing a real
 * boot ROM here.
 *
 * This class only depends on the generic {@link Bus} interface, not on any
 * concrete memory map — it can be tested against a trivial in-memory Bus
 * (see Sh4CpuTest) without needing the rest of the system.
 *
 * <b>Delay slots:</b> real SH-4 hardware executes the instruction
 * immediately following a <i>delayed</i> branch (BRA, and eventually
 * BSR/JMP/JSR/RTS/RTE once implemented) before the branch takes effect.
 * {@code BT}/{@code BF} are NOT delayed branches on real hardware and never
 * have a delay slot. This interpreter now models delay-slot execution for
 * {@code BRA} (the only delayed-branch instruction implemented so far):
 * the instruction at {@code PC+2} is executed first, and only then does
 * {@code PC} jump to the branch target. Placing a branch instruction
 * itself in a delay slot is illegal on real hardware (it raises an
 * "illegal slot instruction" exception); this interpreter throws an
 * {@link IllegalStateException} in that case rather than silently
 * misbehaving.
 *
 * <b>Other known simplifications (tracked as follow-up accuracy work — see
 * /docs/ROADMAP.md):</b>
 * <ul>
 *   <li>Only the small instruction subset implemented in
 *       {@link #executeNonDelayedInstruction} (plus {@code BRA}, handled in
 *       {@link #step()}) is supported; everything else throws
 *       {@link UnsupportedOperationException} with the offending opcode and
 *       address, by design — gaps should be loud, not silently wrong.</li>
 *   <li>The Status Register only models the T ("test"/comparison result)
 *       flag so far; other bits (interrupt mask, privilege mode, etc.) are
 *       not modeled yet.</li>
 * </ul>
 */
public class Sh4Cpu {

    public static final int NUM_GPR = 16;

    /**
     * General-purpose registers R0-R15. By SH-4 convention R15 becomes the
     * stack pointer once code uses it that way; this interpreter doesn't
     * enforce that, it's just software convention.
     */
    public final int[] r = new int[NUM_GPR];

    /** Address of the next instruction to fetch and execute. */
    public int pc;

    /** Procedure register (subroutine return address). Not yet written by any implemented instruction. */
    public int pr;

    /** Status register. Only bit 0 (the T flag) is modeled so far. */
    private int sr;

    private final Bus bus;

    public Sh4Cpu(Bus bus, int initialPc) {
        this.bus = bus;
        this.pc = initialPc;
    }

    public boolean tFlag() {
        return (sr & 1) != 0;
    }

    private void setT(boolean value) {
        sr = value ? (sr | 1) : (sr & ~1);
    }

    /** Current status register value, for tests/debug tooling. Only bit 0 is meaningful so far. */
    public int statusRegister() {
        return sr;
    }

    /**
     * Fetches, decodes, and executes exactly one instruction at {@link #pc},
     * advancing it sequentially, to a branch target, or — for delayed
     * branches like {@code BRA} — first executing the delay-slot
     * instruction and then jumping to the target. See the class Javadoc for
     * delay-slot semantics.
     *
     * @throws UnsupportedOperationException if an opcode isn't one of the
     *         instructions implemented so far
     * @throws IllegalStateException if a delay slot contains a branch
     *         instruction (illegal on real hardware too)
     */
    public void step() {
        int thisPc = pc;
        int opcode = fetch(thisPc);

        if ((opcode & 0xF000) == 0xA000) {
            // BRA label — delayed branch: the instruction at thisPc+2 (the
            // delay slot) executes BEFORE the jump takes effect.
            int disp12 = signExtend12(opcode & 0x0FFF);
            int target = thisPc + 4 + disp12 * 2;
            executeDelaySlot(thisPc + 2);
            pc = target;
            return;
        }

        pc = executeNonDelayedInstruction(thisPc, opcode);
    }

    /**
     * Executes the single instruction in a delayed branch's delay slot.
     * Discards its "natural next PC" — the enclosing branch's target
     * overrides it regardless of what the slot instruction itself would
     * have advanced PC to.
     */
    private void executeDelaySlot(int slotPc) {
        int opcode = fetch(slotPc);
        if (isBranchOpcode(opcode)) {
            throw new IllegalStateException(String.format(
                    "Illegal slot instruction: opcode 0x%04X at PC=0x%08X is a branch " +
                            "and cannot appear in a delay slot", opcode, slotPc));
        }
        executeNonDelayedInstruction(slotPc, opcode);
    }

    private static boolean isBranchOpcode(int opcode) {
        return (opcode & 0xF000) == 0xA000   // BRA
                || (opcode & 0xFF00) == 0x8900  // BT
                || (opcode & 0xFF00) == 0x8B00; // BF
    }

    /**
     * Decodes and executes any implemented instruction EXCEPT the delayed
     * branch {@code BRA} (handled separately in {@link #step()} because of
     * its delay-slot semantics). Returns the address execution should
     * continue at for non-branching/non-delayed instructions (either
     * {@code thisPc + 2}, or a branch target for the non-delayed {@code BT}/
     * {@code BF} instructions).
     */
    private int executeNonDelayedInstruction(int thisPc, int opcode) {
        int n = (opcode >> 8) & 0xF;
        int m = (opcode >> 4) & 0xF;
        int imm8 = opcode & 0xFF;
        int nextPc = thisPc + 2;

        if (opcode == 0x0009) {
            // NOP — no operation.
        } else if ((opcode & 0xF000) == 0xE000) {
            // MOV #imm,Rn — load sign-extended 8-bit immediate.
            r[n] = signExtend8(imm8);
        } else if ((opcode & 0xF00F) == 0x6003) {
            // MOV Rm,Rn
            r[n] = r[m];
        } else if ((opcode & 0xF000) == 0x7000) {
            // ADD #imm,Rn — Rn += sign-extended 8-bit immediate.
            r[n] = r[n] + signExtend8(imm8);
        } else if ((opcode & 0xF00F) == 0x300C) {
            // ADD Rm,Rn
            r[n] = r[n] + r[m];
        } else if ((opcode & 0xF00F) == 0x3008) {
            // SUB Rm,Rn
            r[n] = r[n] - r[m];
        } else if ((opcode & 0xF00F) == 0x3000) {
            // CMP/EQ Rm,Rn — T = (Rn == Rm)
            setT(r[n] == r[m]);
        } else if ((opcode & 0xFF00) == 0x8800) {
            // CMP/EQ #imm,R0 — T = (R0 == sign-extended 8-bit immediate)
            setT(r[0] == signExtend8(imm8));
        } else if ((opcode & 0xFF00) == 0x8900) {
            // BT label — branch if T is set. NOT a delayed branch on real hardware.
            if (tFlag()) {
                nextPc = thisPc + 4 + signExtend8(imm8) * 2;
            }
        } else if ((opcode & 0xFF00) == 0x8B00) {
            // BF label — branch if T is clear. NOT a delayed branch on real hardware.
            if (!tFlag()) {
                nextPc = thisPc + 4 + signExtend8(imm8) * 2;
            }
        } else if ((opcode & 0xF00F) == 0x2002) {
            // MOV.L Rm,@Rn — store Rm's value to the address held in Rn.
            bus.write32(Integer.toUnsignedLong(r[n]), r[m]);
        } else if ((opcode & 0xF00F) == 0x6002) {
            // MOV.L @Rm,Rn — load from the address held in Rm into Rn.
            r[n] = bus.read32(Integer.toUnsignedLong(r[m]));
        } else {
            throw new UnsupportedOperationException(String.format(
                    "Unimplemented SH-4 opcode 0x%04X at PC=0x%08X", opcode, thisPc));
        }

        return nextPc;
    }

    private int fetch(int address) {
        return bus.read16(Integer.toUnsignedLong(address)) & 0xFFFF;
    }

    private static int signExtend8(int value) {
        return (byte) value;
    }

    private static int signExtend12(int value) {
        value &= 0xFFF;
        if ((value & 0x800) != 0) {
            value |= 0xFFFFF000;
        }
        return value;
    }
}
