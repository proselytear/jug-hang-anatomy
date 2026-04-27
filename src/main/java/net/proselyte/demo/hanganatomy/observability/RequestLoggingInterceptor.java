package net.proselyte.demo.hanganatomy.observability;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String START_NANOS_ATTRIBUTE = RequestLoggingInterceptor.class.getName() + ".start";

    private final DemoModeRegistry demoModeRegistry;

    public RequestLoggingInterceptor(DemoModeRegistry demoModeRegistry) {
        this.demoModeRegistry = demoModeRegistry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_NANOS_ATTRIBUTE, System.nanoTime());
        MDC.put(MetricsContract.TAG_MODE, demoModeRegistry.currentMode().token());
        MDC.put(MetricsContract.TAG_ENDPOINT, endpointTag(request));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object startNanos = request.getAttribute(START_NANOS_ATTRIBUTE);
        long durationMs = startNanos instanceof Long started ? (System.nanoTime() - started) / 1_000_000 : -1;
        MDC.put(MetricsContract.TAG_OUTCOME, outcome(response, ex));
        MDC.put("durationMs", Long.toString(durationMs));
        log.info("Handled request");
        MDC.remove("durationMs");
        MDC.remove(MetricsContract.TAG_OUTCOME);
        MDC.remove(MetricsContract.TAG_ENDPOINT);
        MDC.remove(MetricsContract.TAG_MODE);
    }

    private String endpointTag(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern != null) {
            return pattern.toString();
        }
        return request.getRequestURI();
    }

    private String outcome(HttpServletResponse response, Exception ex) {
        if (ex != null) {
            return "exception";
        }
        return response.getStatus() >= 500 ? "server_error" : response.getStatus() >= 400 ? "client_error" : "success";
    }
}
