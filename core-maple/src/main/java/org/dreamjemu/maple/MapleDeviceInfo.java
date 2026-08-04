package org.dreamjemu.maple;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * The {@code deviceinfo} structure a peripheral returns in response to a
 * {@code REQUEST_DEVICE_INFO} command: 28 words (112 bytes) total,
 * describing which functions the peripheral supports and identifying it.
 *
 * <p>Source: Marcus Comstedt, "Dreamcast Programming - Maple Bus"
 * (mc.pp.se/dc/maplebus.html), "Commands" section, {@code deviceinfo...}
 * layout:
 *
 * <pre>
 * int32    func              ; supported function codes, OR'd together (big endian)
 * int32[3] function_data     ; up to 3 function-specific info words (big endian)
 * int8     area_code         ; regional code
 * int8     connector_direction
 * char[30] product_name
 * char[60] product_license
 * int16    standby_power     ; (little endian)
 * int16    max_power         ; (little endian)
 * </pre>
 *
 * <p>4 + 12 + 1 + 1 + 30 + 60 + 2 + 2 = 112 bytes = 28 words, matching the
 * source's "28 words in total".
 *
 * <p>The source page does not state the padding character used to fill
 * unused bytes of {@code product_name}/{@code product_license} shorter
 * than their field width. Both NUL and ASCII space padding are observed
 * in the wild on real hardware/emulator dumps, so {@link #decode} strips
 * either (and any trailing mix of the two) when trimming these fields,
 * and {@link #encode} pads with NUL, the more common convention.
 */
public record MapleDeviceInfo(int functionCodes, int[] functionData, byte areaCode, byte connectorDirection,
                               String productName, String productLicense, short standbyPower, short maxPower) {

    public static final int ENCODED_LENGTH = 112;

    private static final int PRODUCT_NAME_LENGTH = 30;
    private static final int PRODUCT_LICENSE_LENGTH = 60;

    public MapleDeviceInfo {
        if (functionData.length != 3) {
            throw new IllegalArgumentException("functionData must have exactly 3 elements, got " + functionData.length);
        }
        functionData = functionData.clone();
        requireFits("productName", productName, PRODUCT_NAME_LENGTH);
        requireFits("productLicense", productLicense, PRODUCT_LICENSE_LENGTH);
    }

    private static void requireFits(String fieldName, String value, int maxLength) {
        int byteLength = value.getBytes(StandardCharsets.US_ASCII).length;
        if (byteLength > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " is " + byteLength + " bytes, exceeds the " + maxLength + "-byte field");
        }
    }

    @Override
    public int[] functionData() {
        return functionData.clone();
    }

    // A plain record's generated equals()/hashCode() would compare the
    // functionData array by reference (int[] doesn't override Object's
    // identity-based equals/hashCode), silently breaking equality for any
    // two logically-identical instances built from separate arrays -
    // exactly the kind of easy-to-miss bug this project's hand-tracing
    // discipline exists to catch. Overridden explicitly with Arrays
    // element-wise comparison instead.
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MapleDeviceInfo that)) {
            return false;
        }
        return functionCodes == that.functionCodes
                && Arrays.equals(functionData, that.functionData)
                && areaCode == that.areaCode
                && connectorDirection == that.connectorDirection
                && productName.equals(that.productName)
                && productLicense.equals(that.productLicense)
                && standbyPower == that.standbyPower
                && maxPower == that.maxPower;
    }

    @Override
    public int hashCode() {
        int result = java.util.Objects.hash(functionCodes, areaCode, connectorDirection, productName,
                productLicense, standbyPower, maxPower);
        return 31 * result + Arrays.hashCode(functionData);
    }

    @Override
    public String toString() {
        return "MapleDeviceInfo[functionCodes=0x" + Integer.toHexString(functionCodes)
                + ", functionData=" + Arrays.toString(functionData)
                + ", areaCode=" + areaCode
                + ", connectorDirection=" + connectorDirection
                + ", productName=" + productName
                + ", productLicense=" + productLicense
                + ", standbyPower=" + standbyPower
                + ", maxPower=" + maxPower + "]";
    }

    public byte[] encode() {
        byte[] out = new byte[ENCODED_LENGTH];
        int offset = 0;
        writeInt32BE(out, offset, functionCodes);
        offset += 4;
        for (int word : functionData) {
            writeInt32BE(out, offset, word);
            offset += 4;
        }
        out[offset++] = areaCode;
        out[offset++] = connectorDirection;
        offset = writeFixedAscii(out, offset, productName, PRODUCT_NAME_LENGTH);
        offset = writeFixedAscii(out, offset, productLicense, PRODUCT_LICENSE_LENGTH);
        writeInt16LE(out, offset, standbyPower);
        offset += 2;
        writeInt16LE(out, offset, maxPower);
        offset += 2;
        return out;
    }

    public static MapleDeviceInfo decode(byte[] data, int offset) {
        if (offset < 0 || offset + ENCODED_LENGTH > data.length) {
            throw new IllegalArgumentException("Not enough data to decode a MapleDeviceInfo at offset " + offset);
        }
        int pos = offset;
        int functionCodes = readInt32BE(data, pos);
        pos += 4;
        int[] functionData = new int[3];
        for (int i = 0; i < 3; i++) {
            functionData[i] = readInt32BE(data, pos);
            pos += 4;
        }
        byte areaCode = data[pos++];
        byte connectorDirection = data[pos++];
        String productName = readFixedAscii(data, pos, PRODUCT_NAME_LENGTH);
        pos += PRODUCT_NAME_LENGTH;
        String productLicense = readFixedAscii(data, pos, PRODUCT_LICENSE_LENGTH);
        pos += PRODUCT_LICENSE_LENGTH;
        short standbyPower = readInt16LE(data, pos);
        pos += 2;
        short maxPower = readInt16LE(data, pos);
        return new MapleDeviceInfo(functionCodes, functionData, areaCode, connectorDirection, productName,
                productLicense, standbyPower, maxPower);
    }

    private static void writeInt32BE(byte[] out, int offset, int value) {
        out[offset] = (byte) (value >>> 24);
        out[offset + 1] = (byte) (value >>> 16);
        out[offset + 2] = (byte) (value >>> 8);
        out[offset + 3] = (byte) value;
    }

    private static int readInt32BE(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    private static void writeInt16LE(byte[] out, int offset, short value) {
        out[offset] = (byte) value;
        out[offset + 1] = (byte) (value >>> 8);
    }

    private static short readInt16LE(byte[] data, int offset) {
        return (short) ((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8));
    }

    private static int writeFixedAscii(byte[] out, int offset, String value, int length) {
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, out, offset, bytes.length);
        Arrays.fill(out, offset + bytes.length, offset + length, (byte) 0);
        return offset + length;
    }

    private static String readFixedAscii(byte[] data, int offset, int length) {
        int end = offset + length;
        while (end > offset && (data[end - 1] == 0 || data[end - 1] == ' ')) {
            end--;
        }
        return new String(data, offset, end - offset, StandardCharsets.US_ASCII);
    }
}
