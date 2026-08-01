package org.dreamjemu.common.log;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Uses a capturing {@link LogSink} (no real stdout/file I/O) to verify
 * {@link Logger}'s level filtering and message formatting. {@link LogConfig}
 * is process-wide mutable state shared by every test in the JVM — every
 * test here resets it in {@link #setUp()}/{@link #tearDown()}, so this
 * class can't leak state into (or be affected by) any other test class.
 *
 * <p><b>Caught a real bug 2026-07-31:</b> an earlier version of this class
 * split setup across two separate {@code @BeforeEach} methods (reset config,
 * then separately install the capturing sink). JUnit5 does not guarantee
 * execution order between multiple {@code @BeforeEach} methods in the same
 * class — when the sink-install method happened to run first, the
 * config-reset method's {@code resetSinkToStdout()} call silently undid it,
 * so every record went to real stdout instead of {@code captured}, and
 * every {@code captured.size() == 1} assertion failed while every
 * {@code captured.isEmpty()} assertion passed trivially. Caught by the
 * project owner's own {@code ./gradlew :common:test} run — a class of bug
 * a hand-written, non-JUnit sandbox harness (imperative, so it has no
 * lifecycle-method-ordering concept to get wrong) cannot reproduce, which
 * is exactly why this project keeps flagging "still needs a real Gradle
 * run" in its CHANGELOG rather than treating sandbox verification as
 * equivalent. Fixed by using a single {@code @BeforeEach} method instead.
 */
class LoggerTest {

    private List<LogRecord> captured;

    @BeforeEach
    void setUp() {
        LogConfig.setGlobalLevel(LogLevel.INFO);
        LogConfig.clearAllLevelOverrides();
        captured = new ArrayList<>();
        LogConfig.setSink(captured::add);
    }

    @AfterEach
    void tearDown() {
        LogConfig.setGlobalLevel(LogLevel.INFO);
        LogConfig.clearAllLevelOverrides();
        LogConfig.resetSinkToStdout();
    }

    @Test
    void infoMessagePassesAtTheDefaultLevel() {
        Logger log = Logger.get(LoggerTest.class);

        log.info("hello");

        assertEquals(1, captured.size());
        assertEquals(LogLevel.INFO, captured.get(0).level());
        assertEquals("hello", captured.get(0).message());
        assertEquals("LoggerTest", captured.get(0).loggerName());
    }

    @Test
    void debugMessageIsSuppressedAtTheDefaultInfoLevel() {
        Logger log = Logger.get(LoggerTest.class);

        log.debug("should not appear");

        assertTrue(captured.isEmpty());
    }

    @Test
    void raisingGlobalLevelLetsDebugThrough() {
        LogConfig.setGlobalLevel(LogLevel.DEBUG);
        Logger log = Logger.get(LoggerTest.class);

        log.debug("now visible");

        assertEquals(1, captured.size());
        assertEquals(LogLevel.DEBUG, captured.get(0).level());
    }

    @Test
    void offSuppressesEverythingIncludingError() {
        LogConfig.setGlobalLevel(LogLevel.OFF);
        Logger log = Logger.get(LoggerTest.class);

        log.error("should still not appear");

        assertTrue(captured.isEmpty());
    }

    @Test
    void varargsOverloadFormatsTheMessage() {
        Logger log = Logger.get(LoggerTest.class);

        log.info("PC=0x%08X opcode=0x%04X", 0x8C010000, 0x4F22);

        assertEquals(1, captured.size());
        assertEquals("PC=0x8C010000 opcode=0x4F22", captured.get(0).message());
    }

    @Test
    void perLoggerOverrideIsIndependentOfGlobalLevel() {
        // Global stays at INFO (the default); only this one logger name is raised.
        LogConfig.setLevel("NoisyModule", LogLevel.TRACE);
        Logger noisy = Logger.get("NoisyModule");
        Logger quiet = Logger.get("QuietModule");

        noisy.trace("visible");
        quiet.trace("suppressed");

        assertEquals(1, captured.size());
        assertEquals("NoisyModule", captured.get(0).loggerName());
        assertEquals("visible", captured.get(0).message());
    }

    @Test
    void clearingAPerLoggerOverrideRevertsToGlobalLevel() {
        LogConfig.setLevel("NoisyModule", LogLevel.TRACE);
        LogConfig.clearLevel("NoisyModule");
        Logger noisy = Logger.get("NoisyModule");

        noisy.trace("suppressed again, back to global INFO");

        assertTrue(captured.isEmpty());
    }

    @Test
    void isTraceEnabledReflectsTheCurrentLevel() {
        Logger log = Logger.get(LoggerTest.class);
        assertFalse(log.isTraceEnabled(), "TRACE should be disabled at the default INFO level");

        LogConfig.setGlobalLevel(LogLevel.TRACE);
        assertTrue(log.isTraceEnabled());
    }

    @Test
    void errorOverloadWithThrowableCarriesItThrough() {
        Logger log = Logger.get(LoggerTest.class);
        RuntimeException cause = new RuntimeException("boom");

        log.error("something failed", cause);

        assertEquals(1, captured.size());
        assertEquals(cause, captured.get(0).throwable());
    }

    @Test
    void perLoggerSystemPropertyOverrideAppliesWhenTheLoggerIsCreated() {
        String propertyKey = "dreamjemu.log.level.PropertyConfiguredModule";
        System.setProperty(propertyKey, "TRACE");
        try {
            Logger log = Logger.get("PropertyConfiguredModule"); // global stays at INFO

            log.trace("visible because of the system property");

            assertEquals(1, captured.size());
        } finally {
            System.clearProperty(propertyKey);
            LogConfig.clearLevel("PropertyConfiguredModule");
        }
    }

    @Test
    void formatIncludesLevelLoggerNameAndMessage() {
        LogRecord record = new LogRecord(java.time.Instant.now(), "SomeModule", LogLevel.WARN, "careful", null);

        String formatted = record.format();

        assertTrue(formatted.contains("WARN"));
        assertTrue(formatted.contains("SomeModule"));
        assertTrue(formatted.contains("careful"));
    }
}
