package org.dreamjemu.maple;

/**
 * The frame header that starts every Maple bus frame: one 32-bit word,
 * big-endian, laid out as command/response code, recipient address,
 * sender address, and the number of additional (parameter/data) words
 * that follow it in the frame.
 *
 * <p>Source: Marcus Comstedt, "Dreamcast Programming - Maple Bus"
 * (mc.pp.se/dc/maplebus.html), "Frame structure" section:
 *
 * <pre>
 * bits 31-24 : command / response code
 * bits 23-16 : recipient address
 * bits 15-8  : sender address
 * bits 7-0   : number of additional words in the frame
 * </pre>
 *
 * <p>Note this is the logical big-endian frame header as it would sit in
 * memory (e.g. as read back from a Maple DMA result buffer already
 * assembled by hardware) — it is deliberately not concerned with the
 * additional byte-swapping the physical wire protocol applies to packet
 * data (see mc.pp.se/dc/maplewire.html), which is out of scope for this
 * class.
 */
public record MapleFrameHeader(MapleCommand command, int recipientAddress, int senderAddress,
                                int additionalWordCount) {

    public MapleFrameHeader {
        if ((recipientAddress & ~0xFF) != 0) {
            throw new IllegalArgumentException("recipientAddress must fit in a byte: " + recipientAddress);
        }
        if ((senderAddress & ~0xFF) != 0) {
            throw new IllegalArgumentException("senderAddress must fit in a byte: " + senderAddress);
        }
        if ((additionalWordCount & ~0xFF) != 0) {
            throw new IllegalArgumentException("additionalWordCount must fit in a byte: " + additionalWordCount);
        }
    }

    /** Packs this header into its 32-bit big-endian wire representation. */
    public int encode() {
        return ((command.code() & 0xFF) << 24)
                | ((recipientAddress & 0xFF) << 16)
                | ((senderAddress & 0xFF) << 8)
                | (additionalWordCount & 0xFF);
    }

    /** Same as {@link #encode()}, as the 4 big-endian bytes that would sit at the start of the frame on the wire/in a buffer. */
    public byte[] encodeBytes() {
        int word = encode();
        return new byte[] {(byte) (word >>> 24), (byte) (word >>> 16), (byte) (word >>> 8), (byte) word};
    }

    /** Unpacks a 32-bit big-endian frame header word. */
    public static MapleFrameHeader decode(int word) {
        MapleCommand command = MapleCommand.fromCode((byte) (word >>> 24));
        int recipient = (word >>> 16) & 0xFF;
        int sender = (word >>> 8) & 0xFF;
        int additionalWords = word & 0xFF;
        return new MapleFrameHeader(command, recipient, sender, additionalWords);
    }
}
