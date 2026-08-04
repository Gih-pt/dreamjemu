package org.dreamjemu.maple;

/**
 * Maple bus command / response codes, as carried in the top byte of a
 * {@link MapleFrameHeader}. Positive codes are commands and success
 * responses; negative codes are error responses.
 *
 * <p>Source: Marcus Comstedt, "Dreamcast Programming - Maple Bus"
 * (mc.pp.se/dc/maplebus.html), "Commands" section. Verified against
 * that page before implementation.
 */
public enum MapleCommand {

    REQUEST_DEVICE_INFO(1),
    REQUEST_EXTENDED_DEVICE_INFO(2),
    RESET_DEVICE(3),
    SHUTDOWN_DEVICE(4),
    DEVICE_INFO(5),
    EXTENDED_DEVICE_INFO(6),
    COMMAND_ACK(7),
    DATA_TRANSFER(8),
    GET_CONDITION(9),
    GET_MEMORY_INFO(10),
    BLOCK_READ(11),
    BLOCK_WRITE(12),
    SET_CONDITION(14),
    NO_RESPONSE(-1),
    FUNCTION_CODE_UNSUPPORTED(-2),
    UNKNOWN_COMMAND(-3),
    RESEND_REQUEST(-4),
    FILE_ERROR(-5);

    private final int code;

    MapleCommand(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    /**
     * Looks up the command matching a raw wire code.
     *
     * <p>Deliberately throws on an unrecognized code rather than returning
     * a placeholder: an unrecognized command/response code means either a
     * malformed frame or a real gap in this enum, and both should fail
     * loudly rather than be silently misinterpreted (same "gaps are loud"
     * discipline {@code Sh4Cpu} follows for unimplemented opcodes).
     */
    public static MapleCommand fromCode(int code) {
        for (MapleCommand command : values()) {
            if (command.code == code) {
                return command;
            }
        }
        throw new IllegalArgumentException("Unknown Maple command/response code: " + code);
    }
}
