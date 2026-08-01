package org.dreamjemu.common.log;

import java.io.PrintStream;

/**
 * Where log output actually goes. The default (see {@link LogConfig}) writes
 * to {@link System#out}, which is all a CLI tool needs — but this is
 * intentionally an interface, not a hardcoded destination, so a future UI
 * (e.g. {@code app-javafx}'s planned debug/diagnostic console — see
 * /docs/ROADMAP.md) can install its own sink (a text pane, a ring buffer for
 * an in-app log viewer, etc.) without any change to the modules doing the
 * logging.
 */
@FunctionalInterface
public interface LogSink {

    void accept(LogRecord record);

    /** A sink that writes each record's {@link LogRecord#format()} as one line to {@code out}. */
    static LogSink toStream(PrintStream out) {
        return record -> out.println(record.format());
    }
}
