package org.dreamjemu.common.log;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central, mutable logging configuration shared by every {@link Logger}
 * instance across every module: the effective severity threshold (global,
 * with optional per-logger overrides) and where output actually goes (the
 * {@link LogSink}).
 *
 * <p><b>Initial level:</b> read once, at class-init time, from — in order of
 * precedence — the {@code dreamjemu.log.level} system property, then the
 * {@code DREAMJEMU_LOG_LEVEL} environment variable, then {@link LogLevel#INFO}
 * if neither is set. A system property is easy to pass via {@code -D} to any
 * JVM launch (including {@code ./gradlew :app-cli:run -Ddreamjemu.log.level=DEBUG
 * --args="..."}); the environment variable exists because that's often more
 * convenient when just running a packaged jar directly. Invalid/unrecognized
 * values fall back to {@link LogLevel#INFO} rather than failing startup.
 *
 * <p>{@code app-cli} additionally exposes this as a plain {@code --log-level}
 * command-line flag — see {@code Main}'s usage text — since neither a system
 * property nor an environment variable is an obvious thing for someone to
 * reach for on a CLI tool they just downloaded.
 */
public final class LogConfig {

    private static final String LEVEL_PROPERTY = "dreamjemu.log.level";
    private static final String LEVEL_ENV_VAR = "DREAMJEMU_LOG_LEVEL";
    private static final String PER_LOGGER_LEVEL_PROPERTY_PREFIX = "dreamjemu.log.level.";

    private static volatile LogLevel globalLevel = readInitialGlobalLevel();
    private static volatile LogSink sink = LogSink.toStream(System.out);
    private static final Map<String, LogLevel> perLoggerLevel = new ConcurrentHashMap<>();

    private LogConfig() {
    }

    /** Sets the global severity threshold. Loggers with no per-logger override (the common case) use this directly. */
    public static void setGlobalLevel(LogLevel level) {
        globalLevel = level;
    }

    public static LogLevel globalLevel() {
        return globalLevel;
    }

    /**
     * Overrides the effective level for one specific logger name (see
     * {@link Logger#get(Class)}), independent of {@link #globalLevel()} —
     * e.g. running everything at {@code INFO} except a single noisy
     * {@code TRACE}-level SH-4 instruction trace.
     */
    public static void setLevel(String loggerName, LogLevel level) {
        perLoggerLevel.put(loggerName, level);
    }

    /** Removes a previously-set per-logger override, reverting that logger to {@link #globalLevel()}. */
    public static void clearLevel(String loggerName) {
        perLoggerLevel.remove(loggerName);
    }

    /** Removes every per-logger override. Does not change {@link #globalLevel()}. */
    public static void clearAllLevelOverrides() {
        perLoggerLevel.clear();
    }

    /** Where log output goes. Swappable — see {@link LogSink}'s Javadoc for why (e.g. a future UI console). */
    public static void setSink(LogSink newSink) {
        sink = newSink;
    }

    /** Resets output to the default (writing to {@link System#out}) — mainly useful for tests that install a capturing sink. */
    public static void resetSinkToStdout() {
        sink = LogSink.toStream(System.out);
    }

    static boolean isEnabled(String loggerName, LogLevel level) {
        LogLevel threshold = perLoggerLevel.getOrDefault(loggerName, globalLevel);
        return level.ordinal() >= threshold.ordinal();
    }

    static void dispatch(String loggerName, LogLevel level, String message, Throwable throwable) {
        sink.accept(new LogRecord(Instant.now(), loggerName, level, message, throwable));
    }

    private static LogLevel readInitialGlobalLevel() {
        String fromProperty = System.getProperty(LEVEL_PROPERTY);
        LogLevel parsed = parseLevelOrNull(fromProperty);
        if (parsed != null) {
            return parsed;
        }
        String fromEnv = System.getenv(LEVEL_ENV_VAR);
        parsed = parseLevelOrNull(fromEnv);
        return parsed != null ? parsed : LogLevel.INFO;
    }

    private static LogLevel parseLevelOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LogLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Package-visible accessor so {@link Logger} can read a per-logger-name system property override at construction time. */
    static LogLevel perLoggerSystemPropertyOverride(String loggerName) {
        return parseLevelOrNull(System.getProperty(PER_LOGGER_LEVEL_PROPERTY_PREFIX + loggerName));
    }
}
