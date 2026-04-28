package net.proselyte.demo.hanganatomy.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import net.proselyte.demo.hanganatomy.config.HangDemoProperties;
import net.proselyte.demo.hanganatomy.dto.AdminStatusResponse;
import net.proselyte.demo.hanganatomy.entity.AdminStatusRecord;
import net.proselyte.demo.hanganatomy.observability.DemoMode;
import net.proselyte.demo.hanganatomy.observability.DemoModeRegistry;
import net.proselyte.demo.hanganatomy.repository.AdminStatusRepository;
import net.proselyte.demo.hanganatomy.service.AdminStatusService;
import net.proselyte.demo.hanganatomy.service.ServiceUnavailableException;
import org.junit.jupiter.api.Test;

class AdminStatusServiceTest {

    @Test
    void returnsDatabaseValueWithoutCaching() {
        AdminStatusRepository repository = mock(AdminStatusRepository.class);
        DemoModeRegistry modeRegistry = mock(DemoModeRegistry.class);
        when(modeRegistry.currentMode()).thenReturn(DemoMode.NORMAL);
        when(repository.loadCurrent()).thenReturn(new AdminStatusRecord(1L, "GREEN", Instant.parse("2026-01-01T10:00:00Z")));

        HangDemoProperties properties = TestPropertiesFactory.defaults();
        AdminStatusService service = new AdminStatusService(repository, modeRegistry, properties);

        AdminStatusResponse first = service.getStatus();
        AdminStatusResponse second = service.getStatus();

        assertThat(first.cached()).isFalse();
        assertThat(second.cached()).isFalse();
        assertThat(second.status()).isEqualTo("GREEN");
    }

    @Test
    void propagatesDatabaseFailureWithoutFallback() {
        AdminStatusRepository repository = mock(AdminStatusRepository.class);
        DemoModeRegistry modeRegistry = mock(DemoModeRegistry.class);
        when(modeRegistry.currentMode()).thenReturn(DemoMode.NORMAL);
        when(repository.loadCurrent()).thenThrow(new ServiceUnavailableException("db unavailable"));

        AdminStatusService service = new AdminStatusService(repository, modeRegistry, TestPropertiesFactory.defaults());

        assertThatThrownBy(service::getStatus)
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("db unavailable");
    }
}
