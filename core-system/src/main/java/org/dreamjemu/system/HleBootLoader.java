package org.dreamjemu.system;

/**
 * Loads a Dreamcast boot executable's raw bytes — typically
 * {@code 1ST_READ.BIN}, located and read via {@code core-gdrom}'s
 * {@code IpBinHeader} + {@code Iso9660FileSystem} (see
 * {@code Iso9660FileSystem#readFile}) — into main RAM at the address real
 * Dreamcast hardware loads it to, and reports the entry point address the
 * SH-4 should be started at next.
 *
 * This is the last piece of the project's initial BIOS-free HLE boot
 * sequence (see /docs/ROADMAP.md): {@code IpBinHeader} names the boot file,
 * {@code Iso9660FileSystem} locates and reads it, and this class places it
 * in memory exactly where the real boot ROM would, so {@link
 * org.dreamjemu.cpu.sh4.Sh4Cpu} (not referenced directly here, to keep
 * core-system decoupled from core-cpu-sh4 — see /docs/ARCHITECTURE.md) can
 * simply be constructed with the returned address as its initial PC. This
 * never requires or reads any real Sega BIOS/firmware file — the load
 * address itself is a publicly documented hardware fact, not something
 * extracted from console hardware.
 *
 * <b>Load/entry address:</b> {@link #BOOT_FILE_ENTRY_ADDRESS} (0x8C010000)
 * is the SH-4 P1 (cache-enabled) virtual address real Dreamcast hardware
 * loads the boot executable to and jumps to once loaded — confirmed against
 * Marcus Comstedt's "Dreamcast Programming - IP.BIN and 1ST_READ.BIN" page
 * (mc.pp.se/dc/ip.bin.html), the same authoritative reference already used
 * for {@code IpBinHeader}'s field offsets. Physically this is {@link
 * DreamcastAddressMap#MAIN_RAM_BASE} + 0x10000 (right after the 0x8000-byte
 * IP.BIN load area) once {@link SystemBus} masks off the P1 cache-select
 * bits via {@link DreamcastAddressMap#PHYSICAL_ADDRESS_MASK} — this class
 * writes through the virtual address directly via the generic {@link Bus}
 * interface, so it works against any {@code Bus} implementation, not just
 * {@link SystemBus}, the same design already used by {@code Sh4Cpu} itself.
 *
 * <b>Known limitation (tracked in /docs/STATUS.md, not handled here):</b>
 * on real CD (not GD-ROM) discs, {@code 1ST_READ.BIN} is stored "scrambled"
 * — a documented scatter-load obfuscation used as copy protection, which
 * the real ROM bootstrap reverses while loading it. This loader always does
 * a single, contiguous, unscrambled load. That's correct for GD-ROM-sourced
 * dumps (the format this project's disc reading primarily targets), but not
 * yet for a boot file that was scrambled at CD mastering time.
 */
public final class HleBootLoader {

    private HleBootLoader() {
    }

    /**
     * The SH-4 P1 virtual address the boot executable is loaded to, and
     * where the CPU should be started once loading is complete. See the
     * class Javadoc for the source backing this value.
     */
    public static final long BOOT_FILE_ENTRY_ADDRESS = 0x8C01_0000L;

    /**
     * A sentinel value the caller should set {@code Sh4Cpu.pr} to before starting execution at
     * {@link #BOOT_FILE_ENTRY_ADDRESS} — <b>not</b> a real Dreamcast hardware value, and
     * deliberately kept as a plain {@code int} constant here (not referencing {@link
     * org.dreamjemu.cpu.sh4.Sh4Cpu} directly) to keep this class decoupled from core-cpu-sh4, the
     * same way the rest of this class already is (see the class Javadoc).
     *
     * <p><b>Why this exists:</b> found necessary by a real Sonic Adventure run (2026-08-04) that
     * stopped at {@code PC=0x00000000} after 12,791,752+ real SH-4 instructions, immediately
     * after an {@code RTS}. Diagnosis (see {@code Sh4Cpu.pr}'s Javadoc and {@code Main}'s own
     * {@code PC=0} diagnostic message): {@code Sh4Cpu.pr} defaults to Java's {@code int} default,
     * {@code 0}, and this BIOS-free HLE boot never sets it before starting execution. On real
     * hardware, the BIOS sets {@code PR} to a valid return address into its own supervisor code
     * before jumping to a game — so when the game's outermost call frame eventually {@code RTS}s
     * back out (which real games do; this isn't unusual), it lands somewhere real hardware
     * actually has code for. This project has no such supervisor code to return into, so instead
     * of leaving {@code PR} at {@code 0} (a fetch address this interpreter can't do anything
     * useful with — opcode {@code 0x0000} isn't a valid instruction, and {@code 0x00000000} isn't
     * a legitimate SH-4 address on real hardware either), the caller sets it to this sentinel
     * instead, so that eventual {@code RTS} lands somewhere a caller can recognize on purpose and
     * report as "execution unwound cleanly out of the boot entry" rather than "hit an
     * unimplemented/invalid address" — the same spirit as {@code BiosSyscallHandler}'s trap
     * addresses (a value chosen by <i>this project</i>, not real hardware, specifically so this
     * interpreter can recognize it and react intentionally).
     *
     * <p>This does rely on {@code PR}'s normal call/return threading actually working correctly
     * for this to reach the sentinel rather than some other stale value: real SH-4 code nests
     * calls by spilling {@code PR} to the stack at function entry ({@code STS.L PR,@-Rn}) and
     * restoring it before its own {@code RTS} ({@code LDS.L @Rn+,PR}) — see {@code docs/STATUS.md}
     * for confirmation both of those are implemented and tested. So long as that chain is intact,
     * whatever {@code PR} holds at the very start of execution (this sentinel, instead of {@code
     * 0}) is exactly what should eventually get restored and returned to, however many calls deep
     * execution goes in between.
     *
     * <p>Chosen as {@code 0xFFFFFFFF} (all bits set) specifically because: it's never a legitimate
     * SH-4 fetch address on this hardware (every real P0-P3 area this project's {@code
     * DreamcastAddressMap} models tops out well below it), and it reads unambiguously as "this
     * was never a real address to begin with" to anyone inspecting a register dump — distinct
     * from {@code 0}, which could plausibly be mistaken for "just never got set" rather than
     * "recognized on purpose."
     */
    public static final int BOOT_RETURN_SENTINEL = 0xFFFFFFFF;

    /**
     * The SH-4 P1 virtual address {@code Sh4Cpu.r[15]} (the stack pointer) should be set to
     * before starting execution at {@link #BOOT_FILE_ENTRY_ADDRESS} — the very top of main RAM's
     * P1 mirror, i.e. one past the last valid RAM byte, matching the universal convention (real
     * BIOS/IP.BIN bootstrap code, KallistiOS's own crt0, and every other Dreamcast HLE-boot
     * implementation this project is aware of) of a stack that starts at the top of RAM and
     * grows downward via predecrement (e.g. {@code STS.L PR,@-Rn}).
     *
     * <p><b>Why this exists:</b> found necessary by the same real Sonic Adventure run that led to
     * {@link #BOOT_RETURN_SENTINEL} above, once that sentinel's own diagnostic logging (see
     * {@code Sh4Cpu}'s {@code LDS.L @Rn+,PR} comment) revealed the actual root cause: {@code
     * Sh4Cpu.r[15]} — like {@code PR} — also defaults to Java's {@code int} default, {@code 0},
     * and this BIOS-free HLE boot never set it either. Every {@code STS.L Xxx,@-R15}-style push
     * from an uninitialized {@code R15=0} wraps around (32-bit unsigned arithmetic) to a huge
     * address near {@code 0xFFFFFFFF} instead of a real RAM address — confirmed directly by that
     * run's diagnostic log, which showed {@code R15} at exactly {@code 0xFFFFFFBC} (i.e. {@code
     * -68}, i.e. {@code -(17*4)}) after 17 unmatched 4-byte pushes since boot. A subsequent {@code
     * LDS.L @Rn+,PR} reading from that wrapped-around address is what was actually loading {@code
     * PR} back to {@code 0} (or whatever this project's {@code Bus} returns for that
     * unmapped/differently-mapped high address) — not any issue with {@link
     * #BOOT_RETURN_SENTINEL} itself, which was working correctly for the case it was designed
     * for; it just never got a chance to matter here, since the stack was corrupting other data
     * (including, eventually, saved copies of {@code PR}) well before the boot entry's outermost
     * {@code RTS} was ever reached.
     *
     * <p>Value: {@code DreamcastAddressMap.MAIN_RAM_BASE + MAIN_RAM_SIZE}, plus the same
     * {@code 0x80000000} P1 (cache-enabled virtual) offset {@link #BOOT_FILE_ENTRY_ADDRESS}
     * already uses — i.e. {@code 0x8D000000}. Confirmed against the documented main-RAM window
     * (hitmen.c02.at/files/docs/dc/memory.html: RAM occupies {@code 0x8c000000}-{@code
     * 0x8d000000}, 16 MB) — the same "top of the mapped RAM window" reasoning used everywhere in
     * the Dreamcast homebrew community for an initial stack pointer, rather than a value read off
     * any single specific source's literal assembly, since no single canonical "the BIOS sets SP
     * to exactly this" citation was found; this is standard practice, not a documented syscall
     * return value the way {@link #BOOT_RETURN_SENTINEL} itself is not (see that constant's own
     * Javadoc for why it, by contrast, is explicitly NOT modeling real hardware behavior).
     */
    public static final int INITIAL_STACK_POINTER =
            (int) (0x8000_0000L + DreamcastAddressMap.MAIN_RAM_BASE + DreamcastAddressMap.MAIN_RAM_SIZE);

    /**
     * Writes {@code bootFileBytes} into {@code bus} starting at {@link
     * #BOOT_FILE_ENTRY_ADDRESS}, one byte at a time (making no assumption
     * about how the target {@link Bus} implementation stores its backing
     * memory).
     *
     * @return {@link #BOOT_FILE_ENTRY_ADDRESS}, narrowed to {@code int} —
     *         ready to pass directly as {@code Sh4Cpu}'s initial PC, which
     *         already treats every address as a 32-bit {@code int}
     *         internally (see {@code Sh4Cpu#fetch}, which widens back via
     *         {@code Integer.toUnsignedLong} before every bus access)
     */
    public static int loadBootFile(Bus bus, byte[] bootFileBytes) {
        for (int i = 0; i < bootFileBytes.length; i++) {
            bus.write8(BOOT_FILE_ENTRY_ADDRESS + i, bootFileBytes[i]);
        }
        return (int) BOOT_FILE_ENTRY_ADDRESS;
    }
}
