package net.proselyte.demo.hanganatomy.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.proselyte.demo.hanganatomy.config.HangDemoProperties;
import net.proselyte.demo.hanganatomy.dto.AdminStatusResponse;
import net.proselyte.demo.hanganatomy.entity.AdminStatusRecord;
import net.proselyte.demo.hanganatomy.observability.DemoMode;
import net.proselyte.demo.hanganatomy.observability.DemoModeRegistry;
import net.proselyte.demo.hanganatomy.repository.AdminStatusRepository;
import net.proselyte.demo.hanganatomy.service.AdminStatusService;
import net.proselyte.demo.hanganatomy.service.ServiceUnavailableException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AdminStatusServiceTest {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @AfterEach
    void tearDown() throws InterruptedException {
        executorService.shutdownNow();
        executorService.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    void returnsStaleCachedStatusWhenDatabaseFails() {
        AdminStatusRepository repository = mock(AdminStatusRepository.class);
        DemoModeRegistry modeRegistry = mock(DemoModeRegistry.class);
        when(modeRegistry.currentMode()).thenReturn(DemoMode.NORMAL);
        when(repository.loadCurrent())
                .thenReturn(new AdminStatusRecord(1L, "GREEN", Instant.parse("2026-01-01T10:00:00Z")))
                .thenThrow(new ServiceUnavailableException("db unavailable"));

        HangDemoProperties base = TestPropertiesFactory.defaults();
        HangDemoProperties properties = new HangDemoProperties(
                base.demo(), base.partner(), base.db(), base.rateLimiter(),
                new HangDemoProperties.Admin(java.time.Duration.ZERO, java.time.Duration.ofMillis(100), Map.of()),
                base.health()
        );

        AdminStatusService service = new AdminStatusService(repository, modeRegistry, properties, new SimpleMeterRegistry(), executorService);

        AdminStatusResponse first = service.getStatus();
        AdminStatusResponse second = service.getStatus();

        assertThat(first.cached()).isFalse();
        assertThat(second.cached()).isTrue();
        assertThat(second.status()).isEqualTo("GREEN");
    }
}
