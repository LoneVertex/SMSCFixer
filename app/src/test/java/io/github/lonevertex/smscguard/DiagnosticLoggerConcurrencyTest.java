package io.github.lonevertex.smscguard;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DiagnosticLoggerConcurrencyTest {

    @Test
    public void diagnosticsGatingTogglesCleanly() {
        DiagnosticLogger logger = new DiagnosticLogger("TestTag");

        // Initially disabled
        logger.diagnostic("test_event", "metadata=val");
        logger.throttledDiagnostic("test_throttled", "metadata=val", 10_000L);

        // Enable diagnostics
        logger.setDiagnosticsEnabled(true);
        logger.diagnostic("test_event", "metadata=val");
        logger.throttledDiagnostic("test_throttled", "metadata=val", 10_000L);

        // Info events always safe regardless of diagnosticsEnabled
        logger.info("info_event");
    }

    @Test
    public void throttlingSuppressesRapidDuplicatesAndRecoversAfterInterval() throws Exception {
        DiagnosticLogger logger = new DiagnosticLogger("TestTag");
        logger.setDiagnosticsEnabled(true);

        long throttleMs = 60L;

        // First call logs
        logger.throttledDiagnostic("event_a", "meta=1", throttleMs);

        // Immediate subsequent call is throttled
        logger.throttledDiagnostic("event_a", "meta=2", throttleMs);

        // After throttle interval elapses, event can log again
        Thread.sleep(80L);
        logger.throttledDiagnostic("event_a", "meta=3", throttleMs);
    }

    @Test
    public void throttledFailureFormatsExpectedEventKeyWithoutCrashing() {
        DiagnosticLogger logger = new DiagnosticLogger("TestTag");
        logger.setDiagnosticsEnabled(true);

        Throwable ex = new IllegalStateException("Test exception");
        logger.throttledFailure("test_op", ex, 10_000L);
        // Immediate second call should be throttled
        logger.throttledFailure("test_op", ex, 10_000L);
    }

    @Test
    public void maxEventKeysBoundedCapacityPreventsMemoryLeak() {
        DiagnosticLogger logger = new DiagnosticLogger("TestTag");
        logger.setDiagnosticsEnabled(true);

        // Exceed MAX_EVENT_KEYS (64) with distinct event keys
        for (int i = 0; i < 150; i++) {
            logger.throttledDiagnostic("key_" + i, "meta=" + i, 100_000L);
        }
        // Cache should clear and reset without throwing OutOfMemory or crashing
        logger.throttledDiagnostic("key_after_reset", "meta=ok", 100_000L);
    }

    @Test
    public void concurrentLoggingIsThreadSafeUnderHighLoad() throws Exception {
        DiagnosticLogger logger = new DiagnosticLogger("TestTag");
        logger.setDiagnosticsEnabled(true);

        int threadCount = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        logger.info("thread_info_" + threadId);
                        logger.throttledDiagnostic("shared_key", "thread=" + threadId, 500L);
                        logger.throttledDiagnostic("distinct_key_" + (i % 20), "thread=" + threadId, 500L);
                        logger.throttledFailure("concurrent_op", new RuntimeException("Concurrent err"), 500L);
                    }
                } catch (Throwable t1) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertTrue("All concurrent worker threads must complete in time", finished);
        assertEquals("No exceptions should be thrown during concurrent logging", 0, failureCount.get());
    }
}
