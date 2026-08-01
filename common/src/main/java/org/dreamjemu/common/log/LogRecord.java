package org.dreamjemu.common.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * A single log event, already fully resolved (message formatted, level
 * checked) by the time a {@link LogSink} receives it — sinks never need to
 * do their own level filtering or message formatting, just decide where
 * {@link #format()} (or their own rendering of these fields) ends up.
 *
 * @param timestamp  when this record was created
 * @param loggerName the originating {@link Logger}'s name (see {@link Logger#get(Class)})
 * @param level      never {@link LogLevel#OFF} — that's a threshold-only value, not a real level
 * @param message    the fully-formatted message (placeholders already substituted)
 * @param throwable  optional; null if this record has no associated exception
 */
public record LogRecord(Instant timestamp, String loggerName, LogLevel level, String message, Throwable throwable) {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /** Human-readable single-line-plus-optional-stack-trace rendering, used by {@link LogSink#toStream}. */
    public String format() {
        String line = String.format("%s [%-5s] %-24s %s",
                TIME_FORMAT.format(timestamp.atZone(ZoneId.systemDefault())), level, loggerName, message);
        if (throwable == null) {
            return line;
        }
        StringWriter stackTrace = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stackTrace));
        return line + System.lineSeparator() + stackTrace;
    }
}
