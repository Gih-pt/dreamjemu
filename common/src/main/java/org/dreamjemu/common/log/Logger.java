package org.dreamjemu.common.log;

/**
 * A named, per-class log facade — get one via {@link #get(Class)} and keep
 * it in a {@code private static final} field, the same convention as
 * SLF4J/java.util.logging and every other common Java logging facade.
 *
 * <p><b>Cheap when disabled, on purpose:</b> every {@code trace}/{@code debug}/
 * etc. method checks {@link LogConfig#isEnabled} FIRST and returns
 * immediately if the level isn't active — the message (and, for the
 * varargs overloads, {@link String#format}) is only ever constructed if the
 * record will actually be emitted. This matters here specifically because
 * {@code core-cpu-sh4} logs at {@link LogLevel#TRACE} on every single SH-4
 * instruction it executes (see its call site), and a real disc image can
 * mean millions of steps (see {@code app-cli}'s step budget) — at any level
 * above {@code TRACE} that path must cost next to nothing.
 *
 * <p><b>One caveat for true hot-path callers:</b> a varargs call like
 * {@code trace("PC=0x%08X", pc)} still allocates an {@code Object[]} and
 * boxes {@code pc} into an {@code Integer} BEFORE this class ever gets a
 * chance to check the level — Java evaluates arguments before the call.
 * For a call site this hot (once per instruction, potentially millions of
 * times), wrap it in {@code if (logger.isTraceEnabled()) { ... }} as well
 * — see {@code Sh4Cpu.step()} for the pattern. Everywhere else, the
 * built-in level check is enough on its own.
 *
 * <p>This class has no third-party dependency (see /docs/DEPENDENCIES.md's
 * "prefer something already available" rule) — it's deliberately small,
 * not a general-purpose logging framework.
 */
public final class Logger {

    private final String name;

    private Logger(String name) {
        this.name = name;
        LogLevel override = LogConfig.perLoggerSystemPropertyOverride(name);
        if (override != null) {
            LogConfig.setLevel(name, override);
        }
    }

    /** Names the logger after {@code clazz.getSimpleName()} — e.g. {@code Logger.get(Sh4Cpu.class)} logs as {@code "Sh4Cpu"}. */
    public static Logger get(Class<?> clazz) {
        return new Logger(clazz.getSimpleName());
    }

    /** Names the logger explicitly, for the rare case a class name isn't the right grouping (e.g. a named subsystem spanning several classes). */
    public static Logger get(String name) {
        return new Logger(name);
    }

    public String name() {
        return name;
    }

    public boolean isTraceEnabled() {
        return LogConfig.isEnabled(name, LogLevel.TRACE);
    }

    public boolean isDebugEnabled() {
        return LogConfig.isEnabled(name, LogLevel.DEBUG);
    }

    public void trace(String message) {
        log(LogLevel.TRACE, message, null);
    }

    public void trace(String format, Object... args) {
        logFormatted(LogLevel.TRACE, format, args);
    }

    public void debug(String message) {
        log(LogLevel.DEBUG, message, null);
    }

    public void debug(String format, Object... args) {
        logFormatted(LogLevel.DEBUG, format, args);
    }

    public void info(String message) {
        log(LogLevel.INFO, message, null);
    }

    public void info(String format, Object... args) {
        logFormatted(LogLevel.INFO, format, args);
    }

    public void warn(String message) {
        log(LogLevel.WARN, message, null);
    }

    public void warn(String message, Throwable throwable) {
        log(LogLevel.WARN, message, throwable);
    }

    public void warn(String format, Object... args) {
        logFormatted(LogLevel.WARN, format, args);
    }

    public void error(String message) {
        log(LogLevel.ERROR, message, null);
    }

    public void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message, throwable);
    }

    public void error(String format, Object... args) {
        logFormatted(LogLevel.ERROR, format, args);
    }

    private void logFormatted(LogLevel level, String format, Object... args) {
        if (!LogConfig.isEnabled(name, level)) {
            return;
        }
        log(level, String.format(format, args), null);
    }

    private void log(LogLevel level, String message, Throwable throwable) {
        if (!LogConfig.isEnabled(name, level)) {
            return;
        }
        LogConfig.dispatch(name, level, message, throwable);
    }
}
