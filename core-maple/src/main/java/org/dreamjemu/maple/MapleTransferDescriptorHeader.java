package org.dreamjemu.maple;

/**
 * The 2-word header that precedes each outgoing Maple frame in a
 * Maple-DMA command table (the table {@link MapleDmaCommandTableAddress}
 * points to). A full command table is a sequence of
 * {@code header word 1, header word 2, N frame data words}, repeated,
 * with the last entry's header having {@code lastDescriptor} set.
 *
 * <p>Source: Sega's "Dreamcast/Dev.Box System Architecture" manual
 * (segaretro.org/images/7/78/DreamcastDevBoxSystemArchitecture.pdf),
 * §2.6.8 "Peripheral Data Transfers". Unlike most of this project's
 * register/structure classes, the manual does <b>not</b> give this
 * header an explicit bit-field table — the layout below is reverse
 * engineered from two full worked examples in that section, each
 * showing 4 real (value → documented meaning) pairs (8 in total,
 * covering all 4 ports, both a 1-word and 2-word frame length, and the
 * last-entry flag toggling correctly only on the final entry of each
 * list):
 *
 * <pre>
 * bit  31    : Last Descriptor (0x00000000 vs 0x80030000 on the same
 *              port/length otherwise - only ever set on the final entry
 *              of a command list in both worked examples)
 * bits 17-16 : Port (0x00010000=port 1, 0x00020000=port 2,
 *              0x80030000=port 3 all confirmed against their prose
 *              "Port N" labels)
 * bits 7-0   : Word count minus 1 (0x00000000/0x00010000/0x00020000 all
 *              labeled "4-byte data transmission" = 1 word = field 0;
 *              0x00000001/0x00010001/0x00020001/0x80030001 all labeled
 *              "8-byte data transmission" = 2 words = field 1)
 * </pre>
 *
 * <p><b>The {@code gun} field is on a different footing</b>: bit 9,
 * cited here from Marcus Comstedt's {@code mc.pp.se/dc/maplebus.html}
 * (which names a "GUN" field in this same header, though that page's own
 * bit-layout table extracted with merged/collapsed columns and couldn't
 * be read with full confidence on its own — see the earlier
 * {@code core-maple}/{@code core-gpu-pvr2} entries for other examples of
 * that same extraction problem). Neither official worked example above
 * exercises a light gun peripheral, so bit 9 specifically has weaker
 * confirmation than {@code lastDescriptor}/{@code port}/{@code wordCount},
 * which are pinned down by 8 real official data points each. Flagged
 * here rather than presented with the same confidence as the rest.
 *
 * <p>{@code resultAddress} (the second header word) is not
 * bit-fielded — it's a plain pointer to where the peripheral's response
 * frame gets written, confirmed directly by the worked examples'
 * "reception data storage address" entries.
 */
public record MapleTransferDescriptorHeader(boolean lastDescriptor, int port, boolean gun, int wordCount,
                                             int resultAddress) {

    private static final int LAST_BIT = 31;
    private static final int PORT_SHIFT = 16;
    private static final int PORT_MASK = 0b11;
    private static final int GUN_BIT = 9;
    private static final int LENGTH_MASK = 0xFF;

    public MapleTransferDescriptorHeader {
        if (port < 0 || port > 3) {
            throw new IllegalArgumentException("port must be 0-3, got " + port);
        }
        if (wordCount < 1 || wordCount > 256) {
            throw new IllegalArgumentException("wordCount must be 1-256, got " + wordCount);
        }
    }

    /** The first header word: last-descriptor flag, port, gun flag, word count. */
    public int encodeTransferInfo() {
        int value = (port & PORT_MASK) << PORT_SHIFT;
        if (lastDescriptor) {
            value |= 1 << LAST_BIT;
        }
        if (gun) {
            value |= 1 << GUN_BIT;
        }
        value |= (wordCount - 1) & LENGTH_MASK;
        return value;
    }

    /** The second header word: the raw result/receive-buffer address. */
    public int encodeResultAddress() {
        return resultAddress;
    }

    public static MapleTransferDescriptorHeader decode(int transferInfoWord, int resultAddressWord) {
        boolean lastDescriptor = ((transferInfoWord >>> LAST_BIT) & 1) != 0;
        int port = (transferInfoWord >>> PORT_SHIFT) & PORT_MASK;
        boolean gun = ((transferInfoWord >>> GUN_BIT) & 1) != 0;
        int wordCount = (transferInfoWord & LENGTH_MASK) + 1;
        return new MapleTransferDescriptorHeader(lastDescriptor, port, gun, wordCount, resultAddressWord);
    }
}
