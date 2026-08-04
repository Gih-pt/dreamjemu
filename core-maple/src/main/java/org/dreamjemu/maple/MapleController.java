package org.dreamjemu.maple;

/**
 * A simulated standard Dreamcast controller peripheral: holds its own
 * Maple bus address, {@link MapleDeviceInfo}, and current
 * {@link ControllerCondition}, and turns an incoming request frame into
 * the correctly-encoded response frame a real controller would send.
 *
 * <p>Deliberately has no dependency on {@code core-system}'s {@code Bus}
 * or any DMA/transfer-descriptor simulation — same "narrow, independently
 * testable" principle {@code core-cpu-sh4}'s {@code Sh4Cpu} follows
 * against the generic {@code Bus} interface (see
 * {@code core-cpu-sh4/build.gradle.kts}). Wiring this into an actual
 * simulated Maple bus/DMA transfer against {@code core-system} is a
 * tracked next step, not attempted here (see {@code docs/STATUS.md}).
 *
 * <p>Only {@code REQUEST_DEVICE_INFO} and {@code GET_CONDITION} (for the
 * Controller function) are handled so far — the minimum needed for a
 * game's boot-time device enumeration and per-frame input polling. Other
 * commands correctly fall back to {@code UNKNOWN_COMMAND} rather than
 * being silently ignored, matching this project's "gaps are loud, not
 * silently wrong" discipline (see {@code Sh4Cpu}).
 */
public final class MapleController {

    private final int address;
    private final MapleDeviceInfo deviceInfo;
    private ControllerCondition condition;

    public MapleController(int address, MapleDeviceInfo deviceInfo, ControllerCondition initialCondition) {
        this.address = address;
        this.deviceInfo = deviceInfo;
        this.condition = initialCondition;
    }

    /** A controller with a plausible, project-authored device-info block (not scraped from a real dump). */
    public static MapleController standard(int address) {
        MapleDeviceInfo info = new MapleDeviceInfo(
                MapleFunctionCode.CONTROLLER,
                new int[] {0, 0, 0},
                (byte) 0xFF,
                (byte) 0,
                "DreamJEmu Simulated Controller",
                "Produced By or Under License From SEGA ENTERPRISES,LTD.",
                (short) 0,
                (short) 0);
        return new MapleController(address, info, ControllerCondition.neutral());
    }

    public int address() {
        return address;
    }

    public ControllerCondition condition() {
        return condition;
    }

    public void setCondition(ControllerCondition condition) {
        this.condition = condition;
    }

    /**
     * Handles one request frame addressed to this device and returns the
     * full encoded response frame: the header word followed by any
     * additional data words, exactly as they'd sit in a Maple DMA result
     * buffer.
     *
     * @param request     the request's decoded frame header
     * @param requestData the request's additional words, as raw bytes (empty if none)
     */
    public byte[] handleRequest(MapleFrameHeader request, byte[] requestData) {
        return switch (request.command()) {
            case REQUEST_DEVICE_INFO -> respondWithDeviceInfo(request);
            case GET_CONDITION -> respondToGetCondition(request, requestData);
            default -> errorResponse(request, MapleCommand.UNKNOWN_COMMAND, new byte[0]);
        };
    }

    private byte[] respondWithDeviceInfo(MapleFrameHeader request) {
        byte[] info = deviceInfo.encode();
        return frame(MapleCommand.DEVICE_INFO, request, info);
    }

    private byte[] respondToGetCondition(MapleFrameHeader request, byte[] requestData) {
        if (requestData.length < 4) {
            throw new IllegalArgumentException("GET_CONDITION request is missing its func parameter word");
        }
        int func = readInt32BE(requestData, 0);
        if (func != MapleFunctionCode.CONTROLLER) {
            return errorResponse(request, MapleCommand.FUNCTION_CODE_UNSUPPORTED, new byte[0]);
        }
        // Response data for command 9 (Get condition) is "func, cond..." per
        // mc.pp.se/dc/maplebus.html's Commands table - the func word is
        // echoed back ahead of the condition structure itself.
        byte[] cond = condition.encode();
        byte[] data = new byte[4 + cond.length];
        writeInt32BE(data, 0, func);
        System.arraycopy(cond, 0, data, 4, cond.length);
        return frame(MapleCommand.DATA_TRANSFER, request, data);
    }

    private byte[] errorResponse(MapleFrameHeader request, MapleCommand errorCommand, byte[] data) {
        return frame(errorCommand, request, data);
    }

    private byte[] frame(MapleCommand responseCommand, MapleFrameHeader request, byte[] data) {
        if (data.length % 4 != 0) {
            throw new IllegalStateException("Maple response data must be a whole number of words, got " + data.length + " bytes");
        }
        MapleFrameHeader header = new MapleFrameHeader(responseCommand, request.senderAddress(), address, data.length / 4);
        byte[] headerBytes = header.encodeBytes();
        byte[] out = new byte[headerBytes.length + data.length];
        System.arraycopy(headerBytes, 0, out, 0, headerBytes.length);
        System.arraycopy(data, 0, out, headerBytes.length, data.length);
        return out;
    }

    private static int readInt32BE(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    private static void writeInt32BE(byte[] out, int offset, int value) {
        out[offset] = (byte) (value >>> 24);
        out[offset + 1] = (byte) (value >>> 16);
        out[offset + 2] = (byte) (value >>> 8);
        out[offset + 3] = (byte) value;
    }
}
