package net.proselyte.demo.hanganatomy.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.proselyte.demo.hanganatomy.controller.DashboardController;
import net.proselyte.demo.hanganatomy.dto.DashboardResponse;
import net.proselyte.demo.hanganatomy.observability.DemoModeRegistry;
import net.proselyte.demo.hanganatomy.observability.MetricsContract;
import net.proselyte.demo.hanganatomy.observability.RequestLoggingInterceptor;
import net.proselyte.demo.hanganatomy.service.DashboardService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestOutcomeLoggingTest {

    @ParameterizedTest
    @ValueSource(strings = {"success", "fallback-bulkhead", "fallback-timeout", "fallback-circuit-open"})
    void logsTheDashboardOutcomeEvenWhenHttpStatusIs200(String outcome) {
        UUID id = UUID.randomUUID();
        DashboardService service = mock(DashboardService.class);
        DashboardResponse response = mock(DashboardResponse.class);
        when(service.getDashboard(id)).thenReturn(response);
        when(response.outcome()).thenReturn(outcome);
        DashboardController controller = new DashboardController(service);
        RequestLoggingInterceptor interceptor = new RequestLoggingInterceptor(
                new DemoModeRegistry(TestPropertiesFactory.defaults()));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/dashboard/" + id);
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingInterceptor.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>() {
            @Override
            protected void append(ILoggingEvent event) {
                event.prepareForDeferredProcessing();
                super.append(event);
            }
        };
        appender.start();
        logger.addAppender(appender);
        try {
            interceptor.preHandle(request, httpResponse, controller);
            assertThat(controller.getDashboard(id, request)).isSameAs(response);
            interceptor.afterCompletion(request, httpResponse, controller, null);

            assertThat(httpResponse.getStatus()).isEqualTo(200);
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.getFirst().getMDCPropertyMap()).containsEntry(MetricsContract.TAG_OUTCOME, outcome);
            assertThat(MDC.get(MetricsContract.TAG_OUTCOME)).isNull();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            MDC.clear();
        }
    }
}
