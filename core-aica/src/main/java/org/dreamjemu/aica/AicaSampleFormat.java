package org.dreamjemu.aica;

/**
 * The sample format a channel plays, as selected by the {@code format}
 * field of {@link AicaPlayControl}.
 *
 * <p>Source: "Yamaha AICA Sound System Hardware Reference v0.8" by yamato
 * (hitmen.c02.at/files/docs/dc/aica_v08.txt), register {@code 0x0000 -
 * PlayControl}:
 *
 * <pre>
 * format: 0 - 16bit
 *         1 - 8bit
 *         2 - ADPCM 4bit
 *         3 - Looping ADPCM 4bit
 * </pre>
 */
public enum AicaSampleFormat {
    PCM_16BIT(0),
    PCM_8BIT(1),
    ADPCM_4BIT(2),
    ADPCM_4BIT_LOOPING(3);

    private final int fieldValue;

    AicaSampleFormat(int fieldValue) {
        this.fieldValue = fieldValue;
    }

    public int fieldValue() {
        return fieldValue;
    }

    public static AicaSampleFormat fromFieldValue(int fieldValue) {
        for (AicaSampleFormat format : values()) {
            if (format.fieldValue == fieldValue) {
                return format;
            }
        }
        throw new IllegalArgumentException("AICA sample format field must be 0-3, got " + fieldValue);
    }
}
