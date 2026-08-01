package org.dreamjemu.common.log;

/**
 * Severity levels for {@link Logger}, ordered from most to least verbose.
 * {@link #OFF} is a threshold value only — nothing is ever logged AT that
 * level, it's just useful as a global/per-logger setting meaning "silent."
 *
 * <p>{@link #TRACE} is intended for genuinely hot-path, high-volume output —
 * for example {@code core-cpu-sh4} logging every single SH-4 instruction it
 * executes. Real disc images run into the millions of instructions (see
 * {@code app-cli}'s step budget), so anything logged at {@code TRACE} must
 * stay cheap to construct when disabled — see {@link Logger}'s Javadoc for
 * how the level check is done before any message formatting happens.
 */
public enum LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    OFF
}
