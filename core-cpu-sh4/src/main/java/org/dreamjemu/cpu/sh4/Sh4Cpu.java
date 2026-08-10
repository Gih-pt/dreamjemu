package org.dreamjemu.cpu.sh4;

import org.dreamjemu.common.log.Logger;
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

    /** Procedure register (subroutine return address). Written by {@code BSR}/{@code JSR}/{@code STS.L PR,@-Rn}; read by {@code RTS}/{@code LDS.L @Rn+,PR}. */
    public int pr;

    /**
     * Vector Base Register — real hardware adds this to a fixed offset to find where to
     * jump on an exception/interrupt (e.g. {@code TRAPA} always jumps to {@code VBR + 0x100}).
     * Starts at 0, same as real hardware out of reset — and, since this project has no BIOS
     * to set it up, {@code TRAPA} will legitimately fail (jumping into unmapped memory) until
     * whatever code is running sets VBR itself via {@code LDC Rn,VBR}, exactly like real
     * boot/runtime-startup code does before it relies on exceptions working.
     */
    public int vbr;

    /**
     * Global Base Register — real hardware uses this as the base for GBR-relative addressing
     * modes ({@code @(disp,GBR)}, {@code @(R0,GBR)}, and the {@code #imm,@(R0,GBR)} logic-op
     * forms), none of which are implemented yet (see docs/STATUS.md's "Not started yet"). Only
     * the register itself — settable/readable via {@code LDC Rn,GBR}/{@code STC GBR,Rn}, found
     * necessary by a real Sonic Adventure dump (opcode {@code 0x0002}, {@code STC SR,Rn}) — exists
     * so far; nothing yet reads it for addressing.
     */
    public int gbr;

    /**
     * TRAPA exception register — real hardware sets this to the {@code TRAPA} instruction's
     * 8-bit immediate, shifted left 2 bits, so a real exception handler (jumped to via
     * {@link #vbr}) can tell which specific trap was requested. Not read by anything in this
     * interpreter yet (there's no handler to read it) — kept for the same reason {@link #ssr}/
     * {@link #spc} are: real, correct state for whenever a caller needs to inspect it.
     */
    public int tra;

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

    /**
     * Single-precision floating-point registers FR0-FR15 (bank 0 only so far — see
     * {@link #fpscr}'s Javadoc: bank-switching via {@code FRCHG}/{@code FPSCR.FR}, and the
     * "extended" back-bank register file (XF0-XF15) it switches to, aren't modeled yet since
     * nothing has needed them). Stored as raw 32-bit patterns (not {@code float}), since so far
     * every FPU instruction this project has needed ({@code FMOV}-family register spill/reload)
     * only moves bits — it doesn't interpret them as IEEE 754 values. A real
     * arithmetic instruction (FADD/FMUL/etc.) will need {@link Float#intBitsToFloat}/
     * {@link Float#floatToRawIntBits} conversions at the point it's added, not here.
     */
    public final int[] fr = new int[16];

    /**
     * Floating-Point Status/Control Register. Controls (among other things not modeled yet —
     * denormalization mode, rounding mode, exception enables) {@code FPSCR.SZ} (bit 20, transfer
     * size: 0 = single-precision {@code FMOV}s move one 32-bit {@code FR} register, 1 =
     * double-precision {@code FMOV}s move a 64-bit {@code DR}/{@code XD} register pair) and
     * {@code FPSCR.FR} (bit 21, which physical bank FR0-FR15 currently addresses — not modeled
     * yet, same reason as {@link #fr}'s Javadoc).
     *
     * <p><b>Deliberately NOT left at Java's {@code int} default of {@code 0}</b> — real hardware
     * resets this to {@code 0x00040001} (confirmed against two independent authoritative
     * sources: the ST/Hitachi "SH-4 32-bit CPU Core Architecture" manual's reset-value table,
     * and multiple Renesas SH7670-series application notes' {@code #define FPSCR_Init
     * 0x00040001}), not {@code 0}. This project has specifically been bitten before by exactly
     * this class of bug ({@link #pr}/{@code r[15]} both silently defaulting to {@code 0} instead
     * of their real reset values — see {@code HleBootLoader.BOOT_RETURN_SENTINEL}/
     * {@code INITIAL_STACK_POINTER}'s Javadoc and docs/STATUS.md for the full story) — this
     * field is initialized correctly from the start specifically to not repeat it. Decoded:
     * {@code SZ=0}, {@code FR=0}, {@code PR=0} (single-precision, bank 0 — matches every
     * {@code FMOV} this project has needed so far), {@code DN=1}, {@code RM=01}. Nothing sets
     * this to anything else yet ({@code LDS Rn,FPSCR}/{@code STS FPSCR,Rn} aren't implemented) —
     * if real code ever changes it before this project implements those, execution will stop on
     * the unimplemented {@code LDS}/{@code STS} opcode rather than silently keeping a stale
     * value, the same "gaps are loud" guarantee as everywhere else in this interpreter.
     */
    public int fpscr = 0x00040001;

    /** Status register. Only bit 0 (the T flag) is modeled so far. */
    private int sr;

    /** Q and M flags, used only by the DIV0U/DIV0S/DIV1 bit-serial division sequence. */
    private boolean qFlag;
    private boolean mFlag;

    private final Bus bus;
    private static final Logger LOG = Logger.get(Sh4Cpu.class);

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
        if (LOG.isTraceEnabled()) {
            // Guarded by isTraceEnabled() so this doesn't pay for a varargs Object[]
            // allocation + int-boxing on every single step() call when TRACE isn't
            // active — Logger.trace(String, Object...) already checks the level
            // internally too, but that check happens AFTER the array/boxing cost the
            // caller (here) already paid to build the arguments. Matters at the
            // volumes real disc images produce — see Logger's Javadoc.
            LOG.trace("PC=0x%08X opcode=0x%04X", thisPc, opcode);
        }

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
        if ((opcode & 0xFF00) == 0x8D00) {
            // BT/S label — delayed branch if T is set: the delay-slot counterpart of
            // the already-implemented (non-delayed) BT. Confirmed against the
            // authoritative SH opcode table (Renesas SH-4A / Hitachi SH7750 manuals),
            // the same sources used for TRAPA/STS.L/LDS.L earlier this session. The
            // branch condition (T) is read NOW, before the delay slot runs — same
            // "decide before delay slot" discipline as JSR/RTS/JMP/RTE above, in case
            // the delay slot itself changes T (e.g. a CMP/TST instruction). Real
            // hardware evaluates the condition at the delayed branch instruction
            // itself, not after its delay slot.
            boolean takeBranch = tFlag();
            int target = thisPc + 4 + signExtend8(opcode & 0xFF) * 2;
            executeDelaySlot(thisPc + 2);
            pc = takeBranch ? target : thisPc + 4;
            return;
        }
        if ((opcode & 0xFF00) == 0x8F00) {
            // BF/S label — delayed branch if T is clear: the delay-slot counterpart of
            // the already-implemented (non-delayed) BF. Found necessary by a real
            // Sonic Adventure dump (opcode 0x8F02, hit after 12,791,622 real SH-4
            // instructions executed correctly — see docs/STATUS.md/CHANGELOG.md). Same
            // "read condition before delay slot" discipline as BT/S above.
            boolean takeBranch = !tFlag();
            int target = thisPc + 4 + signExtend8(opcode & 0xFF) * 2;
            executeDelaySlot(thisPc + 2);
            pc = takeBranch ? target : thisPc + 4;
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
                || (opcode & 0xFF00) == 0x8B00  // BF
                || (opcode & 0xFF00) == 0x8D00  // BT/S
                || (opcode & 0xFF00) == 0x8F00; // BF/S
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

        // Mechanical dispatch across the per-family methods below (extracted from what
        // used to be a single 105-branch if/else chain here -- see docs/STATUS.md /
        // CHANGELOG.md for the restructuring). Each family's internal opcode checks are
        // untouched; a brute-force check across all 65,536 possible 16-bit opcodes
        // confirmed the original chain never had two branches matching the same opcode,
        // so trying families in this fixed order cannot change which instruction fires
        // for any opcode, compared to before this split.
        Integer result;
        if ((result = tryExecuteMisc(thisPc, opcode, n, m, imm8, nextPc)) != null) {
            return result;
        }
        if ((result = tryExecuteDataTransfer(thisPc, opcode, n, m, imm8, nextPc)) != null) {
            return result;
        }
        if ((result = tryExecuteArithmetic(thisPc, opcode, n, m, imm8, nextPc)) != null) {
            return result;
        }
        if ((result = tryExecuteLogic(thisPc, opcode, n, m, imm8, nextPc)) != null) {
            return result;
        }
        if ((result = tryExecuteShiftRotate(thisPc, opcode, n, m, imm8, nextPc)) != null) {
            return result;
        }
        if ((result = tryExecuteExtendSwap(thisPc, opcode, n, m, imm8, nextPc)) != null) {
            return result;
        }
        if ((result = tryExecuteSystemControl(thisPc, opcode, n, m, imm8, nextPc)) != null) {
            return result;
        }
        if ((result = tryExecuteFpu(thisPc, opcode, n, m, imm8, nextPc)) != null) {
            return result;
        }

        throw new UnsupportedOperationException(String.format(
                "Unimplemented SH-4 opcode 0x%04X at PC=0x%08X", opcode, thisPc));
    }

    /**
     * Tries each misc-family instruction. Returns the (possibly
     * updated, e.g. for a taken branch) next PC if {@code opcode} matched one
     * of this family's instructions, or {@code null} if none matched (the
     * caller then tries the next family) -- purely a mechanical split of what
     * used to be one large if/else chain in {@link #executeNonDelayedInstruction};
     * no opcode mask/value/logic differs from before this split.
     */
    private Integer tryExecuteMisc(int thisPc, int opcode, int n, int m, int imm8, int nextPc) {
        if (opcode == 0x0009) {
            // NOP — no operation.
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
        } else {
            return null;
        }
        return nextPc;
    }

    /**
     * Tries each datatransfer-family instruction. Returns the (possibly
     * updated, e.g. for a taken branch) next PC if {@code opcode} matched one
     * of this family's instructions, or {@code null} if none matched (the
     * caller then tries the next family) -- purely a mechanical split of what
     * used to be one large if/else chain in {@link #executeNonDelayedInstruction};
     * no opcode mask/value/logic differs from before this split.
     */
    private Integer tryExecuteDataTransfer(int thisPc, int opcode, int n, int m, int imm8, int nextPc) {
        if ((opcode & 0xF000) == 0xE000) {
            // MOV #imm,Rn — load sign-extended 8-bit immediate.
            r[n] = signExtend8(imm8);
        } else if ((opcode & 0xF00F) == 0x6003) {
            // MOV Rm,Rn
            r[n] = r[m];
        } else if ((opcode & 0xF00F) == 0x2002) {
            // MOV.L Rm,@Rn — store Rm's value to the address held in Rn.
            bus.write32(Integer.toUnsignedLong(r[n]), r[m]);
        } else if ((opcode & 0xF00F) == 0x6002) {
            // MOV.L @Rm,Rn — load from the address held in Rm into Rn.
            r[n] = bus.read32(Integer.toUnsignedLong(r[m]));
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
        } else {
            return null;
        }
        return nextPc;
    }

    /**
     * Tries each arithmetic-family instruction. Returns the (possibly
     * updated, e.g. for a taken branch) next PC if {@code opcode} matched one
     * of this family's instructions, or {@code null} if none matched (the
     * caller then tries the next family) -- purely a mechanical split of what
     * used to be one large if/else chain in {@link #executeNonDelayedInstruction};
     * no opcode mask/value/logic differs from before this split.
     */
    private Integer tryExecuteArithmetic(int thisPc, int opcode, int n, int m, int imm8, int nextPc) {
        if ((opcode & 0xF000) == 0x7000) {
            // ADD #imm,Rn — Rn += sign-extended 8-bit immediate.
            r[n] = r[n] + signExtend8(imm8);
        } else if ((opcode & 0xF00F) == 0x300C) {
            // ADD Rm,Rn
            r[n] = r[n] + r[m];
        } else if ((opcode & 0xF00F) == 0x3008) {
            // SUB Rm,Rn
            r[n] = r[n] - r[m];
        } else if ((opcode & 0xF0FF) == 0x4010) {
            // DT Rn — Rn -= 1; T = (Rn == 0). The SH-4's decrement-and-test loop-counter idiom
            // (paired with BF to loop while nonzero — see the DIV1 examples this codebase
            // already references for the same "count down, test, branch" pattern).
            r[n] = r[n] - 1;
            setT(r[n] == 0);
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
            return null;
        }
        return nextPc;
    }

    /**
     * Tries each logic-family instruction. Returns the (possibly
     * updated, e.g. for a taken branch) next PC if {@code opcode} matched one
     * of this family's instructions, or {@code null} if none matched (the
     * caller then tries the next family) -- purely a mechanical split of what
     * used to be one large if/else chain in {@link #executeNonDelayedInstruction};
     * no opcode mask/value/logic differs from before this split.
     */
    private Integer tryExecuteLogic(int thisPc, int opcode, int n, int m, int imm8, int nextPc) {
        if ((opcode & 0xF00F) == 0x3000) {
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
        } else {
            return null;
        }
        return nextPc;
    }

    /**
     * Tries each shiftrotate-family instruction. Returns the (possibly
     * updated, e.g. for a taken branch) next PC if {@code opcode} matched one
     * of this family's instructions, or {@code null} if none matched (the
     * caller then tries the next family) -- purely a mechanical split of what
     * used to be one large if/else chain in {@link #executeNonDelayedInstruction};
     * no opcode mask/value/logic differs from before this split.
     */
    private Integer tryExecuteShiftRotate(int thisPc, int opcode, int n, int m, int imm8, int nextPc) {
        if ((opcode & 0xF0FF) == 0x4000) {
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
        } else if ((opcode & 0xF0FF) == 0x4008) {
            // SHLL2 Rn — logical shift left by 2. Unlike SHLL/SHAL above, the fixed-amount
            // shift family (SHLL2/8/16, SHLR2/8/16) does NOT touch T on real hardware —
            // confirmed against two independent sources (SH7091 header used by real Dreamcast
            // homebrew, and the authoritative SH opcode table already used this session).
            // Found necessary by a real Sonic Adventure dump (opcode 0x4E08) after it executed
            // 12,791,752 real SH-4 instructions correctly — see docs/STATUS.md/CHANGELOG.md.
            r[n] = r[n] << 2;
        } else if ((opcode & 0xF0FF) == 0x4018) {
            // SHLL8 Rn — logical shift left by 8. No T effect — see SHLL2 above.
            r[n] = r[n] << 8;
        } else if ((opcode & 0xF0FF) == 0x4028) {
            // SHLL16 Rn — logical shift left by 16. No T effect — see SHLL2 above.
            r[n] = r[n] << 16;
        } else if ((opcode & 0xF0FF) == 0x4009) {
            // SHLR2 Rn — logical shift right by 2 (zero-fill). No T effect — see SHLL2 above.
            r[n] = r[n] >>> 2;
        } else if ((opcode & 0xF0FF) == 0x4019) {
            // SHLR8 Rn — logical shift right by 8 (zero-fill). No T effect — see SHLL2 above.
            r[n] = r[n] >>> 8;
        } else if ((opcode & 0xF0FF) == 0x4029) {
            // SHLR16 Rn — logical shift right by 16 (zero-fill). No T effect — see SHLL2 above.
            r[n] = r[n] >>> 16;
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
            // SHAD Rm,Rn — dynamic ARITHMETIC shift. Confirmed against the authoritative SH
            // opcode table (shared-ptr.com/sh_insns.html, the same reference already used for
            // DIV1's Q/M-flag logic) rather than reasoned out from Java's own shift semantics —
            // that reasoning is what produced the bug this replaces (see CHANGELOG).
            //
            // Per the reference's own SHAD() pseudocode, the shift amount is NOT simply "Rm, or
            // -Rm if negative" — it's derived from Rm's low 5 bits specifically:
            //   sgn = Rm & 0x80000000
            //   if (sgn == 0):            Rn <<= (Rm & 0x1F)
            //   else if ((Rm & 0x1F)==0):  Rn = (Rn's MSB set) ? 0xFFFFFFFF : 0   <- FULL sign-fill
            //   else:                      Rn = Rn >> ((~Rm & 0x1F) + 1)          <- arithmetic
            //
            // The middle case (Rm negative AND a multiple of exactly 32, e.g. -32, -64, ...,
            // down to Integer.MIN_VALUE) is the one this interpreter previously got wrong: the
            // old code computed "r[n] >> (-r[m])" directly, and Java's ">>" operator masks its
            // shift count to the low 5 bits for an int operand — so ">> 32" silently became
            // ">> 0", a no-op, instead of the spec's required FULL sign-extension fill. A
            // previous version of this comment reasoned about the Integer.MIN_VALUE edge case
            // and concluded "no special-casing is needed" — that conclusion was wrong: it
            // confirmed the arithmetic coincidence (that case also lands on a Java no-op) without
            // checking it against what the spec actually requires there (full sign-fill, not a
            // no-op). See Sh4CpuTest's shad/shldFullSignFillWhenRmIsExactMultipleOf32* tests,
            // which specifically regression-test this the previous implementation got wrong.
            if ((r[m] & 0x80000000) == 0) {
                r[n] = r[n] << (r[m] & 0x1F);
            } else if ((r[m] & 0x1F) == 0) {
                r[n] = (r[n] & 0x80000000) != 0 ? 0xFFFFFFFF : 0x00000000;
            } else {
                r[n] = r[n] >> ((~r[m] & 0x1F) + 1);
            }
        } else if ((opcode & 0xF00F) == 0x400D) {
            // SHLD Rm,Rn — same shape as SHAD above (same reference, same bug class fixed
            // alongside it), but LOGICAL (zero-filling) rather than arithmetic: the full-fill
            // case always yields 0 (not conditionally 0/0xFFFFFFFF), and the ordinary right-shift
            // case zero-fills instead of sign-extending.
            if ((r[m] & 0x80000000) == 0) {
                r[n] = r[n] << (r[m] & 0x1F);
            } else if ((r[m] & 0x1F) == 0) {
                r[n] = 0;
            } else {
                r[n] = r[n] >>> ((~r[m] & 0x1F) + 1);
            }
        } else {
            return null;
        }
        return nextPc;
    }

    /**
     * Tries each extendswap-family instruction. Returns the (possibly
     * updated, e.g. for a taken branch) next PC if {@code opcode} matched one
     * of this family's instructions, or {@code null} if none matched (the
     * caller then tries the next family) -- purely a mechanical split of what
     * used to be one large if/else chain in {@link #executeNonDelayedInstruction};
     * no opcode mask/value/logic differs from before this split.
     */
    private Integer tryExecuteExtendSwap(int thisPc, int opcode, int n, int m, int imm8, int nextPc) {
        if ((opcode & 0xF00F) == 0x600E) {
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
        } else {
            return null;
        }
        return nextPc;
    }

    /**
     * Tries each systemcontrol-family instruction. Returns the (possibly
     * updated, e.g. for a taken branch) next PC if {@code opcode} matched one
     * of this family's instructions, or {@code null} if none matched (the
     * caller then tries the next family) -- purely a mechanical split of what
     * used to be one large if/else chain in {@link #executeNonDelayedInstruction};
     * no opcode mask/value/logic differs from before this split.
     */
    private Integer tryExecuteSystemControl(int thisPc, int opcode, int n, int m, int imm8, int nextPc) {
        if ((opcode & 0xF0FF) == 0x401B) {
            // TAS.B @Rn — atomic (on real hardware) test-and-set: reads the byte at @Rn,
            // T = (that byte == 0), then writes the byte back with its MSB forced to 1
            // (0x80), regardless of T. The classic single-instruction lock/mutex primitive.
            // This interpreter runs single-threaded, so there's no real bus lock to model —
            // the read-test-write sequence is inherently atomic here already.
            long address = Integer.toUnsignedLong(r[n]);
            byte value = bus.read8(address);
            setT(value == 0);
            bus.write8(address, (byte) (value | 0x80));
        } else if ((opcode & 0xF0FF) == 0x4002) {
            // STS.L MACH,@-Rn — pre-decrement store: Rn -= 4 FIRST, then MACH is written at
            // the new (decremented) Rn. Same "predecrement, then store" shape as MOV.L Rm,@-Rn
            // above, just storing a system register instead of a general-purpose one.
            r[n] -= 4;
            bus.write32(Integer.toUnsignedLong(r[n]), mach);
        } else if ((opcode & 0xF0FF) == 0x4012) {
            // STS.L MACL,@-Rn — same as STS.L MACH,@-Rn above, for MACL.
            r[n] -= 4;
            bus.write32(Integer.toUnsignedLong(r[n]), macl);
        } else if ((opcode & 0xF0FF) == 0x4022) {
            // STS.L PR,@-Rn — same shape again, for PR (the subroutine return address). This
            // is the classic SH-4 function-PROLOGUE instruction: "save my return address onto
            // the stack before I call anything else and clobber PR myself" — almost always the
            // very first or second instruction of any real function that itself calls another.
            r[n] -= 4;
            bus.write32(Integer.toUnsignedLong(r[n]), pr);
        } else if ((opcode & 0xF0FF) == 0x4006) {
            // LDS.L @Rn+,MACH — post-increment load: MACH = value at Rn, THEN Rn += 4. Same
            // "load, then post-increment" shape as MOV.L @Rm+,Rn above, loading into a system
            // register instead of a general-purpose one. Confusingly, this "load" family
            // addresses its pointer register through the "n" field (not "m") despite the
            // mnemonic's "@Rn+" looking like a source operand — verified against the
            // authoritative SH opcode table alongside every other opcode here.
            mach = bus.read32(Integer.toUnsignedLong(r[n]));
            r[n] += 4;
        } else if ((opcode & 0xF0FF) == 0x4016) {
            // LDS.L @Rn+,MACL — same as LDS.L @Rn+,MACH above, for MACL.
            macl = bus.read32(Integer.toUnsignedLong(r[n]));
            r[n] += 4;
        } else if ((opcode & 0xF0FF) == 0x4026) {
            // LDS.L @Rn+,PR — same shape again, for PR. The classic SH-4 function-EPILOGUE
            // instruction: "restore the return address I saved earlier" — the mirror image of
            // STS.L PR,@-Rn above, and normally followed shortly by RTS.
            //
            // Diagnostic added 2026-08-08: a real Sonic Adventure run kept stopping at PC=0
            // right after an RTS even after HleBootLoader.BOOT_RETURN_SENTINEL was set on PR
            // before boot started — meaning execution genuinely reached a point where THIS
            // instruction loaded PR back to 0 from real (guest-addressed) memory, rather than
            // PR simply never having been touched since its initial (sentinel) value. Logging
            // exactly when that happens — which PC, and which source address supplied the 0 —
            // is meant to reveal the actual root cause on the next real run (a stack slot never
            // legitimately written by a matching STS.L PR,@-Rn in this call chain? a real
            // BSR/JSR/RTS bug? something else?) instead of continuing to guess blindly. See
            // docs/STATUS.md's BOOT_RETURN_SENTINEL entry for the full context.
            int sourceAddress = r[n];
            pr = bus.read32(Integer.toUnsignedLong(sourceAddress));
            r[n] += 4;
            if (pr == 0) {
                LOG.warn("LDS.L @Rn+,PR at PC=0x%08X loaded PR=0 from address 0x%08X - this is almost "
                        + "certainly the eventual cause of an RTS landing on an unrecognized PC=0 rather "
                        + "than HleBootLoader.BOOT_RETURN_SENTINEL (see docs/STATUS.md)",
                        thisPc, sourceAddress);
            }
        } else if ((opcode & 0xF0FF) == 0x402E) {
            // LDC Rn,VBR — sets the Vector Base Register that TRAPA (and every other
            // exception/interrupt) jumps relative to. Real boot/runtime-startup code sets this
            // very early, before relying on TRAPA or interrupts working at all — see VBR's Javadoc.
            vbr = r[n];
        } else if ((opcode & 0xF0FF) == 0x0022) {
            // STC VBR,Rn — reads VBR back into a general-purpose register. Mirror image of
            // LDC Rn,VBR above; added alongside it purely for symmetry/testability, the same
            // way this project has consistently implemented STS.L/LDS.L pairs together.
            r[n] = vbr;
        } else if ((opcode & 0xF0FF) == 0x400E) {
            // LDC Rn,SR — loads the Status Register from a general-purpose register. Only bit 0
            // (T) is meaningful in this interpreter's simplified SR model (see sr's Javadoc) —
            // any other bits real code sets (privilege mode, interrupt mask, etc.) are stored but
            // otherwise inert here, the same simplification already accepted for TRAPA's SSR/RTE.
            sr = r[n];
        } else if ((opcode & 0xF0FF) == 0x0002) {
            // STC SR,Rn — reads SR back into a general-purpose register. Found necessary by a
            // real Sonic Adventure dump (opcode 0x0002, R0) after it executed 12,791,785 real
            // SH-4 instructions correctly — see docs/STATUS.md/CHANGELOG.md. Implemented alongside
            // LDC Rn,SR/LDC Rn,GBR/STC GBR,Rn for the same reason every instruction pair/family
            // has been implemented together this session.
            r[n] = sr;
        } else if ((opcode & 0xF0FF) == 0x401E) {
            // LDC Rn,GBR — sets the Global Base Register that GBR-relative addressing modes will
            // eventually use (not implemented yet — see GBR's Javadoc and docs/STATUS.md's "Not
            // started yet"). Storing the register itself now is a reasonable, narrow first step,
            // the same way VBR existed before TRAPA needed it.
            gbr = r[n];
        } else if ((opcode & 0xF0FF) == 0x0012) {
            // STC GBR,Rn — reads GBR back into a general-purpose register. Mirror image of
            // LDC Rn,GBR above, added alongside it for the same symmetry reason as VBR's pair.
            r[n] = gbr;
        } else if ((opcode & 0xF0FF) == 0x00A3) {
            // OCBP @Rn — "Purge Cache Block": on real hardware, reads the cache block
            // containing the address in Rn; if it's dirty, writes it back to external memory,
            // then invalidates it (if the block isn't cached, or isn't dirty, it's simply
            // invalidated). This interpreter does not model a data cache at all — every write
            // already goes straight to the Bus, so memory is always as up to date as any
            // "cache write-back" could make it — meaning OCBP has no observable effect here: no
            // register, memory, or flag change, exactly the same "hardware does X, but a
            // single-cache-less interpreter is already equivalent to the result" reasoning
            // already used above for TAS.B's locked-bus-cycle simplification.
            //
            // Opcode encoding (0000nnnn10100011) confirmed against two independent authoritative
            // sources that agree exactly: the sh4-dis.c disassembler tables used by multiple
            // independent QEMU forks (e.g. ntddk/temu, aquynh/iVM), and nullDC's own SH-4 opcode
            // table (workhorsylegacy/nulldc-linux, sh4_opcode_list.cpp) — the same kind of
            // cross-check discipline used for every other opcode in this session.
            //
            // Found necessary by the real Sonic Adventure dump (opcode 0x04A3, R4) after the
            // HleBootLoader.INITIAL_STACK_POINTER fix let it execute 12,793,102 real SH-4
            // instructions correctly — see docs/STATUS.md/CHANGELOG.md.
            // No-op: intentionally falls through to `return nextPc` at the bottom.
        } else if ((opcode & 0xFF00) == 0xC300) {
            // TRAPA #imm — software exception. Unlike every branch above, TRAPA has NO delay
            // slot (it takes effect immediately) and directly changes PC itself, so — uniquely
            // among the instructions in this method — it returns its own target instead of
            // falling through to `return nextPc` at the bottom.
            //
            // Confirmed against the authoritative SH opcode table (binutils/QEMU's sh4-dis)
            // used for every other opcode added this session: encoding "11000011i8*1", i.e.
            // 0xC300 | imm8.
            //
            // This is the actual mechanism real HLE boot/syscall code uses to invoke
            // BIOS-equivalent functionality (see docs/ROADMAP.md) — but this interpreter has no
            // exception HANDLER (no vector table, no BIOS to have installed one), so all this
            // does, correctly, is perform the hardware-defined entry sequence (save SR/return
            // address, set TRA, jump to VBR+0x100) and stop there; whatever's mapped at that
            // address (nothing, unless the running code set VBR itself and put a real handler
            // there — see VBR's Javadoc) determines what happens next, exactly like real
            // hardware with no BIOS-installed handler.
            // imm8 was already extracted at the top of this method.
            tra = imm8 << 2;
            ssr = sr;
            spc = nextPc;
            return vbr + 0x100;
        } else {
            return null;
        }
        return nextPc;
    }

    /**
     * Tries each FPU-family instruction — a brand-new family (this project's first FPU
     * instruction), not part of the {@code refactor/core-cpu-sh4} mechanical split like the
     * other {@code tryExecute*} methods, since none existed to split at that time.
     * Returns the next PC if {@code opcode} matched, or {@code null} if none matched.
     */
    private Integer tryExecuteFpu(int thisPc, int opcode, int n, int m, int imm8, int nextPc) {
        if ((opcode & 0xF00F) == 0xF00B) {
            // FMOV <FRm/DRm/XDm>,@-Rn — pre-decrement store of a floating-point register (or
            // register pair) to memory, exactly the same "predecrement Rn, then store at the
            // new address" shape as STS.L PR,@-Rn and friends above, just from the FPU register
            // file instead of a system register. Confirmed against two independent authoritative
            // sources that agree exactly: the sh4-dis.c disassembler tables used by multiple
            // independent QEMU forks, and Microsoft's own documented "SH-4 Prolog" reference
            // (which shows real compiler-generated code using precisely this instruction, with
            // R15 as the address register, to spill floating-point argument registers in a
            // function's prologue — exactly the real-world situation this opcode was found in).
            //
            // Real hardware's actual register-file interpretation of the m field depends on
            // FPSCR.SZ at the time this executes (not on the opcode's raw bits alone — the
            // sh4-dis.c table above shows two *possible* disassemblies of the same bit pattern
            // for exactly this reason, since a static disassembler can't know FPSCR.SZ): when
            // SZ=0, m directly addresses one of the 16 single-precision FR registers (4-byte
            // store); when SZ=1, m's low bit distinguishes a DR (bank-0 double, low bit 0) from
            // an XD (always-back-bank double, low bit 1) register pair (8-byte store), with the
            // top 3 bits of m giving the pair index.
            boolean doublePrecision = (fpscr & 0x00100000) != 0; // FPSCR bit 20 (SZ)
            if (!doublePrecision) {
                // The only case actually confirmed needed so far (the real Sonic Adventure dump
                // hit this with FPSCR at its untouched reset value, SZ=0 — see fpscr's own
                // Javadoc for why that's the correct assumption here, not a guess): a plain
                // single-precision FR register spill.
                r[n] -= 4;
                bus.write32(Integer.toUnsignedLong(r[n]), fr[m]);
            } else {
                // Not yet confirmed needed by a real disc run, and genuinely more involved to
                // get right (DR-vs-XD register-bank selection, per fpscr's own Javadoc) — rather
                // than guess at untested double-precision semantics, this gap is left loud
                // instead of silently (and possibly wrongly) implemented. Falls through to the
                // same UnsupportedOperationException every other unimplemented opcode gets, with
                // a clearer message pointing at exactly why.
                throw new UnsupportedOperationException(String.format(
                        "Unimplemented SH-4 opcode 0x%04X at PC=0x%08X (double-precision FMOV "
                                + "Rm,@-Rn — FPSCR.SZ=1 case not implemented yet, only the "
                                + "single-precision case found necessary by a real disc run so "
                                + "far; see Sh4Cpu.tryExecuteFpu's Javadoc)",
                        opcode, thisPc));
            }
        } else if ((opcode & 0xF00F) == 0xF008) {
            // FMOV.S @Rm,FRn — plain register-indirect load, the "read" sibling of the
            // FMOV Rm,@-Rn pre-decrement store directly above (same family, same real-disc
            // prologue context: the Sonic Adventure dump hit this only a handful of
            // instructions after the three FMOV @-R15 stores that let it reach this code at
            // all). No pre/post address modification here — Rm is just read, not changed.
            // Confirmed against multiple independent authoritative sh4-dis.c disassembler
            // tables that agree exactly (including one, Dushistov/qemu_at91sam9263, that
            // already spells it "fmov.s" explicitly rather than the ambiguous general "fmov"
            // some other forks use for this same encoding).
            //
            // Same FPSCR.SZ-dependent register-file interpretation as the store form above (see
            // its comment for the full reasoning) — SZ=0 is the only case confirmed needed so
            // far, for the same reason (FPSCR is still untouched from its documented reset
            // value; no LDS Rn,FPSCR exists in this project yet to have changed it).
            boolean doublePrecision = (fpscr & 0x00100000) != 0; // FPSCR bit 20 (SZ)
            if (!doublePrecision) {
                fr[n] = bus.read32(Integer.toUnsignedLong(r[m]));
            } else {
                throw new UnsupportedOperationException(String.format(
                        "Unimplemented SH-4 opcode 0x%04X at PC=0x%08X (double-precision "
                                + "FMOV @Rm,Rn — FPSCR.SZ=1 case not implemented yet, only the "
                                + "single-precision case found necessary by a real disc run so "
                                + "far; see Sh4Cpu.tryExecuteFpu's Javadoc)",
                        opcode, thisPc));
            }
        } else if ((opcode & 0xF00F) == 0xF007) {
            // FMOV.S FRm,@(R0,Rn) — indexed store: writes FRm to the address R0+Rn (no pre/post
            // modification of either register). Same family as the two FMOV.S forms directly
            // above, found only 5 real instructions later in the same real Sonic Adventure run
            // — likely the same function indexing into an array/struct of floats using R0 as a
            // running offset. Confirmed against six independent authoritative sh4-dis.c
            // disassembler tables that agree exactly, several of which (including
            // Dushistov/qemu_at91sam9263, ntddk/temu) already spell it "fmov.s" explicitly.
            //
            // Same FPSCR.SZ-dependent register-file interpretation as the other two FMOV.S forms
            // above (see the @Rm,FRn form's comment for the full reasoning) — SZ=0 is the only
            // case confirmed needed so far, for the same reason.
            boolean doublePrecision = (fpscr & 0x00100000) != 0; // FPSCR bit 20 (SZ)
            if (!doublePrecision) {
                bus.write32(Integer.toUnsignedLong(r[0] + r[n]), fr[m]);
            } else {
                throw new UnsupportedOperationException(String.format(
                        "Unimplemented SH-4 opcode 0x%04X at PC=0x%08X (double-precision "
                                + "FMOV Rm,@(R0,Rn) — FPSCR.SZ=1 case not implemented yet, only "
                                + "the single-precision case found necessary by a real disc run "
                                + "so far; see Sh4Cpu.tryExecuteFpu's Javadoc)",
                        opcode, thisPc));
            }
        } else {
            return null;
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
