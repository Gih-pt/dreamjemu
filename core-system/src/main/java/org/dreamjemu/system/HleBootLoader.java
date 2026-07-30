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
