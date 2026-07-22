package org.dreamjemu.gdrom;

/**
 * Track type as encoded in a GDI file's per-track line. GD-ROM discs mix
 * audio (CDDA) tracks and data tracks; the GDI format encodes this with a
 * small integer code per track (0 = audio, 4 = data/MODE1), which is the
 * same convention used across the wider open-source Dreamcast tooling
 * ecosystem for this plain-text container format.
 */
public enum GdiTrackType {
    AUDIO(0),
    DATA(4);

    public final int code;

    GdiTrackType(int code) {
        this.code = code;
    }

    public static GdiTrackType fromCode(int code) {
        for (GdiTrackType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown GDI track type code: " + code);
    }
}
