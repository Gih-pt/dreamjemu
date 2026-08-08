package org.dreamjemu.maple;

/**
 * {@code SB_MST} — read-only Maple interface status: whether a transfer
 * is in progress, internal frame/state counters, and the raw
 * input/output line state for each of the 4 ports.
 *
 * <p>Source: Sega's "Dreamcast/Dev.Box System Architecture" manual,
 * §8.4.1.2 "Maple Peripheral Interface":
 *
 * <pre>
 * bit  31    : Move Status (0 = not in operation, 1 = in operation -
 *              received data not yet finalized, transmission data may
 *              not be overwritten)
 * bits 30-27 : Reserved
 * bits 26-24 : Internal Frame Monitor (internal block frame counter)
 * bits 23-22 : Reserved
 * bits 21-16 : Internal State Monitor (internal block state counter)
 * bits 15-8  : Reserved
 * bits 7-0   : Line Monitor (input/output line per port, default 0xFF)
 *              bit7 Port D SDCKA   bit6 Port D SDCKB
 *              bit5 Port C SDCKA   bit4 Port C SDCKB
 *              bit3 Port B SDCKA   bit2 Port B SDCKB
 *              bit1 Port A SDCKA   bit0 Port A SDCKB
 * </pre>
 *
 * <p>Read-only hardware status — only {@link #decode} is provided, no
 * {@code encode()}.
 */
public record MapleStatus(boolean operating, int internalFrameMonitor, int internalStateMonitor, int lineMonitor) {

    /** P2 (uncached) address of this register. */
    public static final int REGISTER_ADDRESS = 0x005F6C84;

    private static final int MOVE_STATUS_BIT = 31;
    private static final int FRAME_MONITOR_SHIFT = 24;
    private static final int FRAME_MONITOR_MASK = 0b111;
    private static final int STATE_MONITOR_SHIFT = 16;
    private static final int STATE_MONITOR_MASK = 0b111111;
    private static final int LINE_MONITOR_MASK = 0xFF;

    public boolean portDSdckaLine() {
        return (lineMonitor & (1 << 7)) != 0;
    }

    public boolean portDSdckbLine() {
        return (lineMonitor & (1 << 6)) != 0;
    }

    public boolean portCSdckaLine() {
        return (lineMonitor & (1 << 5)) != 0;
    }

    public boolean portCSdckbLine() {
        return (lineMonitor & (1 << 4)) != 0;
    }

    public boolean portBSdckaLine() {
        return (lineMonitor & (1 << 3)) != 0;
    }

    public boolean portBSdckbLine() {
        return (lineMonitor & (1 << 2)) != 0;
    }

    public boolean portASdckaLine() {
        return (lineMonitor & (1 << 1)) != 0;
    }

    public boolean portASdckbLine() {
        return (lineMonitor & 1) != 0;
    }

    public static MapleStatus decode(int value) {
        boolean operating = ((value >>> MOVE_STATUS_BIT) & 1) != 0;
        int internalFrameMonitor = (value >>> FRAME_MONITOR_SHIFT) & FRAME_MONITOR_MASK;
        int internalStateMonitor = (value >>> STATE_MONITOR_SHIFT) & STATE_MONITOR_MASK;
        int lineMonitor = value & LINE_MONITOR_MASK;
        return new MapleStatus(operating, internalFrameMonitor, internalStateMonitor, lineMonitor);
    }
}
