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
 * immediately following a <i>delayed</i> branch ({@code BRA}, {@code BSR},
 * {@code JSR}, {@code RTS}, {@code JMP}, and {@code RTE}) before the branch
 * takes effect.
 * {@code BT}/{@code BF} are NOT delayed branches on real hardware and never
 * have a delay slot. This interpreter models delay-slot execution for all
 * currently-implemented delayed branches: the instruction at {@code PC+2}
 * is executed first, and only then does {@code PC} jump to the branch
 * target (for {@code JSR}, the target register is read BEFORE the delay
 * slot runs, in case the delay slot modifies that register — matching real
 * hardware; {@code RTE} similarly reads {@code SSR}/{@code SPC} before its
 * delay slot runs). Placing a branch instruction itself in a delay slot is
 * illegal on real hardware (it raises an "illegal slot instruction"
 * exception); this interpreter throws an {@link IllegalStateException} in
 * that case rather than silently misbehaving.
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
 *       not modeled yet. {@code RTE} restores the whole 32-bit register
 *       wholesale from {@link #ssr} regardless, so this is only a gap for
 *       code that inspects those other bits, not for RTE's own correctness.</li>
 *   <li>There is no real exception/interrupt entry mechanism yet (no
 *       {@code TRAPA}, no interrupt controller, no automatic hardware
 *       save-to-SSR/SPC-and-jump-to-vector on an exception) — only the
 *       {@code RTE} <i>return</i> path is implemented. Tests exercise it by
 *       setting {@link #ssr}/{@link #spc} directly, the same way BSR/JSR/RTS
 *       tests set {@link #pr} directly without a real preceding call.</li>
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

    /**
     * Saved Status Register — on real hardware, the exception/interrupt entry hardware
     * copies the pre-exception SR here automatically, and {@code RTE} copies it back.
     * This interpreter has no exception/interrupt dispatch mechanism yet (no TRAPA, no
     * interrupt controller, no automatic SR/PC save-on-entry) — tests exercise RTE by
     * setting this directly, the same way BSR/JSR/RTS tests set PR directly rather than
     * needing a real "call" to have happened first.
     */
    public int ssr;

    /**
     * Saved Program Counter — on real hardware, the exception/interrupt entry hardware
     * copies the pre-exception return address here automatically, and {@code RTE} jumps
     * back to it. See {@link #ssr}'s Javadoc for how this interpreter currently expects
     * it to be set (directly, not via a real exception-entry mechanism yet).
     */
    public int spc;

    /** High 32 bits of the 64-bit multiply-accumulate result (DMULS.L/DMULU.L). Unused by MUL.L/MULS.W/MULU.W. */
    public int mach;

    /** Low 32 bits of the multiply result — the only part MUL.L/MULS.W/MULU.W write. */
    public int macl;

    /** Status register. Only bit 0 (the T flag) is modeled so far. */
    private int sr;

    /** Q and M flags, used only by the DIV0U/DIV0S/DIV1 bit-serial division sequence. */
    private boolean qFlag;
    private boolean mFlag;

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

    /** The Q flag, as left by the most recent DIV0U/DIV0S/DIV1. */
    public boolean qFlag() {
        return qFlag;
    }

    /** The M flag, as left by the most recent DIV0U/DIV0S. */
    public boolean mFlag() {
        return mFlag;
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
        if ((opcode & 0xF000) == 0xB000) {
            // BSR label — delayed subroutine call. PR gets the return address
            // (thisPc+4, i.e. the address right after the delay slot) BEFORE
            // the delay slot runs, matching real hardware.
            int disp12 = signExtend12(opcode & 0x0FFF);
            int target = thisPc + 4 + disp12 * 2;
            pr = thisPc + 4;
            executeDelaySlot(thisPc + 2);
            pc = target;
            return;
        }
        if ((opcode & 0xF0FF) == 0x400B) {
            // JSR @Rn — delayed subroutine call through a register. The
            // target register is read NOW, before the delay slot executes,
            // in case the delay slot instruction itself modifies Rn.
            int n = (opcode >> 8) & 0xF;
            int target = r[n];
            pr = thisPc + 4;
            executeDelaySlot(thisPc + 2);
            pc = target;
            return;
        }
        if (opcode == 0x000B) {
            // RTS — delayed return: jump to PR, after executing the delay slot.
            int target = pr;
            executeDelaySlot(thisPc + 2);
            pc = target;
            return;
        }
        if ((opcode & 0xF0FF) == 0x402B) {
            // JMP @Rn — delayed unconditional jump through a register (no PR
            // update, unlike JSR). The target register is read NOW, before
            // the delay slot executes, in case the delay slot modifies Rn.
            int n = (opcode >> 8) & 0xF;
            int target = r[n];
            executeDelaySlot(thisPc + 2);
            pc = target;
            return;
        }
        if (opcode == 0x002B) {
            // RTE — delayed return-from-exception: like RTS, but restores BOTH the
            // Status Register (from SSR) and PC (from SPC), after the delay slot
            // executes. Both are read NOW, before the delay slot runs, matching the
            // same "read target before delay slot" discipline as JSR/RTS/JMP above,
            // in case the delay slot itself writes SSR/SPC (e.g. via LDC, not yet
            // implemented, but the ordering should already be correct for when it is).
            int targetPc = spc;
            int targetSr = ssr;
            executeDelaySlot(thisPc + 2);
            sr = targetSr;
            pc = targetPc;
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
                || (opcode & 0xF000) == 0xB000  // BSR
                || (opcode & 0xF0FF) == 0x400B  // JSR
                || (opcode & 0xF0FF) == 0x402B  // JMP
                || opcode == 0x000B             // RTS
                || opcode == 0x002B             // RTE
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
        } else if ((opcode & 0xF00F) == 0x3002) {
            // CMP/HS Rm,Rn — T = (Rn >= Rm), UNSIGNED comparison.
            setT(Integer.compareUnsigned(r[n], r[m]) >= 0);
        } else if ((opcode & 0xF00F) == 0x3003) {
            // CMP/GE Rm,Rn — T = (Rn >= Rm), signed comparison (Java's int is already signed).
            setT(r[n] >= r[m]);
        } else if ((opcode & 0xF00F) == 0x3006) {
            // CMP/HI Rm,Rn — T = (Rn > Rm), UNSIGNED comparison.
            setT(Integer.compareUnsigned(r[n], r[m]) > 0);
        } else if ((opcode & 0xF00F) == 0x3007) {
            // CMP/GT Rm,Rn — T = (Rn > Rm), signed comparison.
            setT(r[n] > r[m]);
        } else if ((opcode & 0xF0FF) == 0x4015) {
            // CMP/PL Rn — T = (Rn > 0), signed.
            setT(r[n] > 0);
        } else if ((opcode & 0xF0FF) == 0x4011) {
            // CMP/PZ Rn — T = (Rn >= 0), signed.
            setT(r[n] >= 0);
        } else if ((opcode & 0xF00F) == 0x200C) {
            // CMP/STR Rm,Rn — T = 1 if ANY of the 4 corresponding bytes of Rn/Rm are equal
            // (useful for zero-terminated-string length/matching: XOR then look for a zero byte).
            int diff = r[n] ^ r[m];
            setT((diff & 0xFF000000) == 0 || (diff & 0x00FF0000) == 0
                    || (diff & 0x0000FF00) == 0 || (diff & 0x000000FF) == 0);
        } else if ((opcode & 0xF00F) == 0x2008) {
            // TST Rm,Rn — T = ((Rn & Rm) == 0). Contents of Rn/Rm unchanged.
            setT((r[n] & r[m]) == 0);
        } else if ((opcode & 0xFF00) == 0xC800) {
            // TST #imm,R0 — T = ((R0 & zero-extended imm) == 0).
            setT((r[0] & (imm8 & 0xFF)) == 0);
        } else if ((opcode & 0xF0FF) == 0x4010) {
            // DT Rn — Rn -= 1; T = (Rn == 0). The SH-4's decrement-and-test loop-counter idiom
            // (paired with BF to loop while nonzero — see the DIV1 examples this codebase
            // already references for the same "count down, test, branch" pattern).
            r[n] = r[n] - 1;
            setT(r[n] == 0);
        } else if ((opcode & 0xF00F) == 0x600E) {
            // EXTS.B Rm,Rn — sign-extend Rm's low BYTE to 32 bits.
            // Java's (byte) cast truncates to the low 8 bits as a signed byte, and assigning
            // that back to an int widens it with sign extension — exactly what's needed here.
            r[n] = (byte) r[m];
        } else if ((opcode & 0xF00F) == 0x600F) {
            // EXTS.W Rm,Rn — sign-extend Rm's low WORD to 32 bits (same (byte)-cast trick, but 16-bit).
            r[n] = (short) r[m];
        } else if ((opcode & 0xF00F) == 0x600C) {
            // EXTU.B Rm,Rn — zero-extend Rm's low BYTE to 32 bits.
            r[n] = r[m] & 0xFF;
        } else if ((opcode & 0xF00F) == 0x600D) {
            // EXTU.W Rm,Rn — zero-extend Rm's low WORD to 32 bits.
            r[n] = r[m] & 0xFFFF;
        } else if ((opcode & 0xF00F) == 0x6008) {
            // SWAP.B Rm,Rn — swaps Rm's low two BYTES; upper 16 bits pass through unchanged.
            int upper16 = r[m] & 0xFFFF0000;
            int lowByte = r[m] & 0xFF;
            int nextByte = (r[m] >>> 8) & 0xFF;
            r[n] = upper16 | (lowByte << 8) | nextByte;
        } else if ((opcode & 0xF00F) == 0x6009) {
            // SWAP.W Rm,Rn — swaps Rm's upper and lower 16-bit halves.
            r[n] = (r[m] << 16) | ((r[m] >>> 16) & 0xFFFF);
        } else if ((opcode & 0xF00F) == 0x200D) {
            // XTRCT Rm,Rn — extracts the middle 32 bits of the 64-bit value formed by
            // concatenating Rm (high 32) and Rn (low 32): Rm's low 16 bits become the result's
            // high 16, and Rn's high 16 bits become the result's low 16.
            r[n] = (r[m] << 16) | ((r[n] >>> 16) & 0xFFFF);
        } else if ((opcode & 0xF00F) == 0x300E) {
            // ADDC Rm,Rn — Rn = Rn + Rm + T (unsigned), carry-out -> T. Used to chain
            // additions wider than 32 bits. Computed via a 64-bit intermediate so the carry
            // is just "did the true sum exceed 32 bits" — semantically identical to the SH-4
            // manual's own two-step unsigned-overflow check, expressed without relying on
            // any Java-specific unsigned-comparison trick.
            long sum = (r[n] & 0xFFFFFFFFL) + (r[m] & 0xFFFFFFFFL) + (tFlag() ? 1 : 0);
            setT(sum > 0xFFFFFFFFL);
            r[n] = (int) sum;
        } else if ((opcode & 0xF00F) == 0x300A) {
            // SUBC Rm,Rn — Rn = Rn - Rm - T (unsigned), borrow-out -> T. The subtraction
            // counterpart of ADDC, for chaining subtractions wider than 32 bits.
            long diff = (r[n] & 0xFFFFFFFFL) - (r[m] & 0xFFFFFFFFL) - (tFlag() ? 1 : 0);
            setT(diff < 0);
            r[n] = (int) diff;
        } else if ((opcode & 0xF00F) == 0x600A) {
            // NEGC Rm,Rn — Rn = 0 - Rm - T (unsigned), borrow-out -> T. Equivalent to SUBC
            // with Rn's "before" value implicitly 0; also usable to sign-invert a value wider
            // than 32 bits (see the SH-4 manual's own worked multi-word negation example).
            long diff = 0L - (r[m] & 0xFFFFFFFFL) - (tFlag() ? 1 : 0);
            setT(diff < 0);
            r[n] = (int) diff;
        } else if ((opcode & 0xF00F) == 0x300F) {
            // ADDV Rm,Rn — Rn += Rm (signed), SIGNED overflow -> T. Direct port of the SH-4
            // manual's own algorithm (same-sign operands producing a different-sign result),
            // rather than a from-scratch reimplementation, since that's the actual documented
            // definition of "signed overflow" here rather than something to re-derive.
            int addDest = (r[n] >= 0) ? 0 : 1;
            int addSrc = (r[m] >= 0) ? 0 : 1;
            addSrc += addDest;
            r[n] = r[n] + r[m];
            int addAns = (r[n] >= 0) ? 0 : 1;
            addAns += addDest;
            setT((addSrc == 0 || addSrc == 2) && addAns == 1);
        } else if ((opcode & 0xF00F) == 0x300B) {
            // SUBV Rm,Rn — Rn -= Rm (signed), SIGNED underflow -> T. Direct port of the SH-4
            // manual's own algorithm, mirroring ADDV above.
            int subDest = (r[n] >= 0) ? 0 : 1;
            int subSrc = (r[m] >= 0) ? 0 : 1;
            subSrc += subDest;
            r[n] = r[n] - r[m];
            int subAns = (r[n] >= 0) ? 0 : 1;
            subAns += subDest;
            setT(subSrc == 1 && subAns == 1);
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
        } else if ((opcode & 0xF00F) == 0x2009) {
            // AND Rm,Rn
            r[n] = r[n] & r[m];
        } else if ((opcode & 0xF00F) == 0x200B) {
            // OR Rm,Rn
            r[n] = r[n] | r[m];
        } else if ((opcode & 0xF00F) == 0x200A) {
            // XOR Rm,Rn
            r[n] = r[n] ^ r[m];
        } else if ((opcode & 0xFF00) == 0xC900) {
            // AND #imm,R0 — logic immediate ops are ZERO-extended, unlike MOV/ADD/CMP's sign-extended immediates.
            r[0] = r[0] & (imm8 & 0xFF);
        } else if ((opcode & 0xFF00) == 0xCB00) {
            // OR #imm,R0 — zero-extended immediate.
            r[0] = r[0] | (imm8 & 0xFF);
        } else if ((opcode & 0xFF00) == 0xCA00) {
            // XOR #imm,R0 — zero-extended immediate.
            r[0] = r[0] ^ (imm8 & 0xFF);
        } else if ((opcode & 0xF0FF) == 0x4000) {
            // SHLL Rn — logical shift left by 1; T = bit shifted out (old MSB).
            setT((r[n] >>> 31 & 1) != 0);
            r[n] = r[n] << 1;
        } else if ((opcode & 0xF0FF) == 0x4001) {
            // SHLR Rn — logical shift right by 1 (zero-fill); T = bit shifted out (old LSB).
            setT((r[n] & 1) != 0);
            r[n] = r[n] >>> 1;
        } else if ((opcode & 0xF0FF) == 0x4020) {
            // SHAL Rn — arithmetic shift left by 1. Identical bit behavior to SHLL on real
            // hardware (there's no difference between logical/arithmetic left shift).
            setT((r[n] >>> 31 & 1) != 0);
            r[n] = r[n] << 1;
        } else if ((opcode & 0xF0FF) == 0x4021) {
            // SHAR Rn — arithmetic shift right by 1 (sign-extending); T = bit shifted out (old LSB).
            setT((r[n] & 1) != 0);
            r[n] = r[n] >> 1;
        } else if ((opcode & 0xF0FF) == 0x4024) {
            // ROTCL Rn — rotate left through T: new T = old MSB; Rn = (Rn<<1) | old T.
            // Used together with DIV1 to fold each computed quotient bit into a
            // separate accumulator register — see the DIV1 handling below.
            boolean newT = (r[n] & 0x80000000) != 0;
            r[n] = (r[n] << 1) | (tFlag() ? 1 : 0);
            setT(newT);
        } else if ((opcode & 0xF0FF) == 0x4025) {
            // ROTCR Rn — rotate right through T: new T = old bit 0; Rn = (Rn>>>1) | (old T << 31).
            // The mirror image of ROTCL above (same 33-bit-rotate-through-T idea, opposite direction).
            boolean newT = (r[n] & 1) != 0;
            r[n] = (r[n] >>> 1) | (tFlag() ? 0x80000000 : 0);
            setT(newT);
        } else if ((opcode & 0xF0FF) == 0x4004) {
            // ROTL Rn — plain (non-T-chained) rotate left: T = old MSB; Rn = (Rn<<1) | T.
            // Unlike ROTCL, the bit rotated out becomes BOTH the new T and the new LSB
            // (it doesn't matter what T held before this ran).
            boolean msb = (r[n] & 0x80000000) != 0;
            r[n] = (r[n] << 1) | (msb ? 1 : 0);
            setT(msb);
        } else if ((opcode & 0xF0FF) == 0x4005) {
            // ROTR Rn — plain rotate right: T = old LSB; Rn = (Rn>>>1) | (T << 31).
            boolean lsb = (r[n] & 1) != 0;
            r[n] = (r[n] >>> 1) | (lsb ? 0x80000000 : 0);
            setT(lsb);
        } else if ((opcode & 0xF00F) == 0x400C) {
            // SHAD Rm,Rn — dynamic ARITHMETIC shift: Rm>0 shifts Rn left by (Rm&0x1F); Rm<0
            // shifts Rn right (sign-extending) by (-Rm&0x1F); Rm==0 leaves Rn unchanged.
            // Java's <<//>> operators already mask their shift count to the low 5 bits for
            // an int operand, which is exactly the "&0x1F" the SH-4 spec calls for — including
            // the edge case Rm==Integer.MIN_VALUE, where "-Rm" itself overflows back to
            // MIN_VALUE in ordinary 32-bit wraparound arithmetic (same as real hardware would
            // do), and MIN_VALUE & 0x1F is 0 either way, so no special-casing is needed here.
            if (r[m] > 0) {
                r[n] = r[n] << r[m];
            } else if (r[m] < 0) {
                r[n] = r[n] >> (-r[m]);
            }
        } else if ((opcode & 0xF00F) == 0x400D) {
            // SHLD Rm,Rn — same as SHAD above, but the right-shift case (Rm<0) is LOGICAL
            // (zero-filling), not arithmetic.
            if (r[m] > 0) {
                r[n] = r[n] << r[m];
            } else if (r[m] < 0) {
                r[n] = r[n] >>> (-r[m]);
            }
        } else if ((opcode & 0xF0FF) == 0x401B) {
            // TAS.B @Rn — atomic (on real hardware) test-and-set: reads the byte at @Rn,
            // T = (that byte == 0), then writes the byte back with its MSB forced to 1
            // (0x80), regardless of T. The classic single-instruction lock/mutex primitive.
            // This interpreter runs single-threaded, so there's no real bus lock to model —
            // the read-test-write sequence is inherently atomic here already.
            long address = Integer.toUnsignedLong(r[n]);
            byte value = bus.read8(address);
            setT(value == 0);
            bus.write8(address, (byte) (value | 0x80));
        } else if ((opcode & 0xF00F) == 0x2000) {
            // MOV.B Rm,@Rn — store the low byte of Rm to the address held in Rn.
            bus.write8(Integer.toUnsignedLong(r[n]), (byte) r[m]);
        } else if ((opcode & 0xF00F) == 0x6000) {
            // MOV.B @Rm,Rn — load a sign-extended byte from the address held in Rm.
            // (bus.read8 returns a Java byte, which auto-sign-extends on assignment to int.)
            r[n] = bus.read8(Integer.toUnsignedLong(r[m]));
        } else if ((opcode & 0xF00F) == 0x2001) {
            // MOV.W Rm,@Rn — store the low 16 bits of Rm to the address held in Rn.
            bus.write16(Integer.toUnsignedLong(r[n]), (short) r[m]);
        } else if ((opcode & 0xF00F) == 0x6001) {
            // MOV.W @Rm,Rn — load a sign-extended 16-bit word from the address held in Rm.
            // (bus.read16 returns a Java short, which auto-sign-extends on assignment to int.)
            r[n] = bus.read16(Integer.toUnsignedLong(r[m]));
        } else if ((opcode & 0xF00F) == 0x6004) {
            // MOV.B @Rm+,Rn — post-increment load: sign-extended byte from @Rm, then Rm += 1.
            // Per the SH-4 spec, if Rn==Rm the increment is SKIPPED (the load overwrites Rm's
            // old value anyway, so hardware doesn't also apply the increment on top of it) —
            // tested explicitly, since assuming "always increment" would silently produce the
            // wrong result in exactly that case.
            r[n] = bus.read8(Integer.toUnsignedLong(r[m]));
            if (n != m) {
                r[m] += 1;
            }
        } else if ((opcode & 0xF00F) == 0x6005) {
            // MOV.W @Rm+,Rn — same as above, a sign-extended 16-bit word; Rm += 2 (skipped if n==m).
            r[n] = bus.read16(Integer.toUnsignedLong(r[m]));
            if (n != m) {
                r[m] += 2;
            }
        } else if ((opcode & 0xF00F) == 0x6006) {
            // MOV.L @Rm+,Rn — same as above, a full 32-bit word; Rm += 4 (skipped if n==m).
            // This is the SH-4's "pop" idiom when Rm is R15 (the stack pointer convention).
            r[n] = bus.read32(Integer.toUnsignedLong(r[m]));
            if (n != m) {
                r[m] += 4;
            }
        } else if ((opcode & 0xF00F) == 0x2004) {
            // MOV.B Rm,@-Rn — pre-decrement store: Rn -= 1 FIRST, then Rm's low byte is
            // written at the new (decremented) Rn. Unlike the post-increment loads above,
            // there is no n==m special case here — the decrement always happens.
            r[n] -= 1;
            bus.write8(Integer.toUnsignedLong(r[n]), (byte) r[m]);
        } else if ((opcode & 0xF00F) == 0x2005) {
            // MOV.W Rm,@-Rn — same as above; Rn -= 2 first.
            r[n] -= 2;
            bus.write16(Integer.toUnsignedLong(r[n]), (short) r[m]);
        } else if ((opcode & 0xF00F) == 0x2006) {
            // MOV.L Rm,@-Rn — same as above; Rn -= 4 first. This is the SH-4's "push" idiom
            // when Rn is R15 (e.g. MOV.L Rm,@-R15 pushes Rm onto the stack).
            r[n] -= 4;
            bus.write32(Integer.toUnsignedLong(r[n]), r[m]);
        } else if ((opcode & 0xF00F) == 0x000C) {
            // MOV.B @(R0,Rm),Rn — indexed load: effective address = Rm + R0.
            r[n] = bus.read8(Integer.toUnsignedLong(r[m] + r[0]));
        } else if ((opcode & 0xF00F) == 0x000D) {
            // MOV.W @(R0,Rm),Rn — indexed load, sign-extended 16-bit.
            r[n] = bus.read16(Integer.toUnsignedLong(r[m] + r[0]));
        } else if ((opcode & 0xF00F) == 0x000E) {
            // MOV.L @(R0,Rm),Rn — indexed load, full 32-bit.
            r[n] = bus.read32(Integer.toUnsignedLong(r[m] + r[0]));
        } else if ((opcode & 0xF00F) == 0x0004) {
            // MOV.B Rm,@(R0,Rn) — indexed store: effective address = Rn + R0.
            bus.write8(Integer.toUnsignedLong(r[n] + r[0]), (byte) r[m]);
        } else if ((opcode & 0xF00F) == 0x0005) {
            // MOV.W Rm,@(R0,Rn) — indexed store, 16-bit.
            bus.write16(Integer.toUnsignedLong(r[n] + r[0]), (short) r[m]);
        } else if ((opcode & 0xF00F) == 0x0006) {
            // MOV.L Rm,@(R0,Rn) — indexed store, 32-bit.
            bus.write32(Integer.toUnsignedLong(r[n] + r[0]), r[m]);
        } else if ((opcode & 0xFF00) == 0x8400) {
            // MOV.B @(disp,Rm),R0 — 4-bit-displacement load, R0-only. disp4 is zero-extended
            // and NOT scaled (byte access), so it reaches +0..+15 bytes from Rm.
            int disp4 = opcode & 0xF;
            r[0] = bus.read8(Integer.toUnsignedLong(r[m] + disp4));
        } else if ((opcode & 0xFF00) == 0x8500) {
            // MOV.W @(disp,Rm),R0 — same, but disp4 is doubled (word access), reaching +0..+30 bytes.
            int disp4 = opcode & 0xF;
            r[0] = bus.read16(Integer.toUnsignedLong(r[m] + disp4 * 2));
        } else if ((opcode & 0xF000) == 0x5000) {
            // MOV.L @(disp,Rm),Rn — 4-bit-displacement load, but (unlike the B/W forms above)
            // Rn is a general register, not just R0. disp4 is quadrupled (longword access),
            // reaching +0..+60 bytes — ideal for reading a nearby struct field.
            int disp4 = opcode & 0xF;
            r[n] = bus.read32(Integer.toUnsignedLong(r[m] + disp4 * 4));
        } else if ((opcode & 0xFF00) == 0x8000) {
            // MOV.B R0,@(disp,Rn) — displacement store (disp4, NOT scaled), R0 only.
            // NOTE: this encoding ("10000000nnnndddd") puts its one register field at
            // bits 4-7 — the same bit position as this method's "m" variable, NOT "n"
            // (bits 8-11, which are always 0000 here) — easy to get backwards since the
            // spec calls that field "Rn"; what matters is the bit position, not the name.
            int disp4 = opcode & 0xF;
            bus.write8(Integer.toUnsignedLong(r[m] + disp4), (byte) r[0]);
        } else if ((opcode & 0xFF00) == 0x8100) {
            // MOV.W R0,@(disp,Rn) — same bit-position note as above; disp4 doubled.
            int disp4 = opcode & 0xF;
            bus.write16(Integer.toUnsignedLong(r[m] + disp4 * 2), (short) r[0]);
        } else if ((opcode & 0xF000) == 0x1000) {
            // MOV.L Rm,@(disp,Rn) — 4-bit-displacement store, general source register,
            // disp4 quadrupled — the store counterpart of the 0x5000 load above.
            int disp4 = opcode & 0xF;
            bus.write32(Integer.toUnsignedLong(r[n] + disp4 * 4), r[m]);
        } else if ((opcode & 0xF000) == 0x9000) {
            // MOV.W @(disp,PC),Rn — PC-relative word load, sign-extended. The 8-bit
            // displacement is zero-extended then doubled; address = thisPc + 4 + disp*2,
            // using THIS instruction's own address (not an already-incremented PC).
            int disp8 = opcode & 0xFF;
            r[n] = bus.read16(Integer.toUnsignedLong(thisPc + 4 + disp8 * 2));
        } else if ((opcode & 0xF000) == 0xD000) {
            // MOV.L @(disp,PC),Rn — PC-relative longword load: the standard way to load a
            // full 32-bit constant, since MOV #imm,Rn only has an 8-bit sign-extended
            // immediate. The 8-bit displacement is zero-extended then quadrupled, and — per
            // the SH-4 spec — the PC value has its low 2 bits masked off FIRST, since the
            // literal pool this addresses is always longword-aligned regardless of whether
            // this instruction itself sits at a 2-mod-4 address.
            int disp8 = opcode & 0xFF;
            r[n] = bus.read32(Integer.toUnsignedLong((thisPc & ~3) + 4 + disp8 * 4));
        } else if ((opcode & 0xFF00) == 0xC700) {
            // MOVA @(disp,PC),R0 — like MOV.L @(disp,PC) above, but loads the EFFECTIVE
            // ADDRESS itself into R0 rather than the data stored there — the standard way to
            // get the address of a literal/string embedded in the code stream. Same
            // PC-masking rule as MOV.L @(disp,PC),Rn.
            int disp8 = opcode & 0xFF;
            r[0] = (thisPc & ~3) + 4 + disp8 * 4;
        } else if ((opcode & 0xF00F) == 0x6007) {
            // NOT Rm,Rn — Rn = bitwise complement of Rm.
            r[n] = ~r[m];
        } else if ((opcode & 0xF00F) == 0x600B) {
            // NEG Rm,Rn — Rn = 0 - Rm (two's-complement negation).
            r[n] = -r[m];
        } else if ((opcode & 0xF00F) == 0x0007) {
            // MUL.L Rm,Rn — 32x32->32 multiply (truncated), result in MACL only.
            macl = r[n] * r[m];
        } else if ((opcode & 0xF00F) == 0x200F) {
            // MULS.W Rm,Rn — signed 16x16->32 multiply (low 16 bits of each register,
            // sign-extended), result in MACL only.
            int rn16 = (short) r[n];
            int rm16 = (short) r[m];
            macl = rn16 * rm16;
        } else if ((opcode & 0xF00F) == 0x200E) {
            // MULU.W Rm,Rn — unsigned 16x16->32 multiply (low 16 bits of each register,
            // zero-extended), result in MACL only.
            int rn16 = r[n] & 0xFFFF;
            int rm16 = r[m] & 0xFFFF;
            macl = rn16 * rm16;
        } else if ((opcode & 0xF00F) == 0x300D) {
            // DMULS.L Rm,Rn — signed 32x32->64 multiply, result in MACH:MACL.
            // Implemented with 64-bit Java long arithmetic rather than the reference
            // 32-bit-limb algorithm — mathematically equivalent, much simpler/less error-prone.
            long product = (long) r[n] * (long) r[m];
            mach = (int) (product >>> 32);
            macl = (int) product;
        } else if ((opcode & 0xF00F) == 0x3005) {
            // DMULU.L Rm,Rn — unsigned 32x32->64 multiply, result in MACH:MACL.
            long product = Integer.toUnsignedLong(r[n]) * Integer.toUnsignedLong(r[m]);
            mach = (int) (product >>> 32);
            macl = (int) product;
        } else if (opcode == 0x0019) {
            // DIV0U — initializes the bit-serial unsigned division sequence: Q = M = T = 0.
            qFlag = false;
            mFlag = false;
            setT(false);
        } else if ((opcode & 0xF00F) == 0x2007) {
            // DIV0S Rm,Rn — initializes the bit-serial signed division sequence:
            // Q = sign of the dividend (Rn), M = sign of the divisor (Rm), T = (Q != M).
            qFlag = (r[n] & 0x80000000) != 0;
            mFlag = (r[m] & 0x80000000) != 0;
            setT(qFlag != mFlag);
        } else if ((opcode & 0xF00F) == 0x3004) {
            // DIV1 Rm,Rn — one step of the bit-serial division algorithm; called 32 times
            // (with a rotate instruction folding T into the running quotient bit-by-bit)
            // to compute a full 32-bit division. See docs/ROADMAP.md — a full round-trip
            // division test needs ROTCL, not implemented yet, so this is tested as a
            // standalone primitive against hand-verified expected state instead.
            boolean oldQ = qFlag;
            qFlag = (r[n] & 0x80000000) != 0;
            int divisor = r[m];
            r[n] = (r[n] << 1) | (tFlag() ? 1 : 0);

            if (!oldQ) {
                if (!mFlag) {
                    int before = r[n];
                    r[n] = r[n] - divisor;
                    boolean borrowed = Integer.compareUnsigned(r[n], before) > 0;
                    qFlag = qFlag ? !borrowed : borrowed;
                } else {
                    int before = r[n];
                    r[n] = r[n] + divisor;
                    boolean carried = Integer.compareUnsigned(r[n], before) < 0;
                    qFlag = qFlag ? carried : !carried;
                }
            } else {
                if (!mFlag) {
                    int before = r[n];
                    r[n] = r[n] + divisor;
                    boolean carried = Integer.compareUnsigned(r[n], before) < 0;
                    qFlag = qFlag ? !carried : carried;
                } else {
                    int before = r[n];
                    r[n] = r[n] - divisor;
                    boolean borrowed = Integer.compareUnsigned(r[n], before) > 0;
                    qFlag = qFlag ? borrowed : !borrowed;
                }
            }
            setT(qFlag == mFlag);
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
