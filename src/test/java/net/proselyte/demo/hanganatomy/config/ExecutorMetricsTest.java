package net.proselyte.demo.hanganatomy.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class ExecutorMetricsTest {

    @Test
    void partnerMetricsMeasureActiveTasksAndActualQueue() throws InterruptedException {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        try {
            ExecutorService executor = new RestClientConfig().partnerExecutor(registry);
            CountDownLatch started = new CountDownLatch(16);
            CountDownLatch release = new CountDownLatch(1);
            try {
                for (int i = 0; i < 16; i++) {
                    executor.submit(() -> awaitRelease(started, release));
                }
                assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                executor.submit(() -> awaitRelease(new CountDownLatch(0), release));

                assertThat(registry.get("executor.active").tag("name", "partner-executor").gauge().value()).isEqualTo(16);
                assertThat(registry.get("executor.queued").tag("name", "partner-executor").gauge().value()).isEqualTo(1);
            } finally {
                release.countDown();
                executor.shutdownNow();
                assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            }
        } finally {
            registry.close();
        }
    }

    @Test
    void budgetMetricsDescribeCachedPoolWithoutAnInternalQueue() throws InterruptedException {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        try {
            ExecutorService executor = new RestClientConfig().budgetExecutor(registry);
            CountDownLatch started = new CountDownLatch(3);
            CountDownLatch release = new CountDownLatch(1);
            try {
                for (int i = 0; i < 3; i++) {
                    executor.submit(() -> awaitRelease(started, release));
                }
                assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

                assertThat(registry.get("executor.active").tag("name", "budget-executor").gauge().value()).isEqualTo(3);
                assertThat(registry.get("executor.queued").tag("name", "budget-executor").gauge().value()).isZero();
            } finally {
                release.countDown();
                executor.shutdownNow();
                assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            }
        } finally {
            registry.close();
        }
    }

    private static void awaitRelease(CountDownLatch started, CountDownLatch release) {
        started.countDown();
        try {
            release.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
