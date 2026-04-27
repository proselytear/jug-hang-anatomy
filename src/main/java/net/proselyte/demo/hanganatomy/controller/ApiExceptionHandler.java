package net.proselyte.demo.hanganatomy.controller;

import java.time.Instant;

import net.proselyte.demo.hanganatomy.dto.ErrorResponse;
import net.proselyte.demo.hanganatomy.service.DashboardNotFoundException;
import net.proselyte.demo.hanganatomy.service.DependencyTimeoutException;
import net.proselyte.demo.hanganatomy.service.HeavyRequestRejectedException;
import net.proselyte.demo.hanganatomy.service.ServiceUnavailableException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DashboardNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(DashboardNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return response(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(DependencyTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeout(DependencyTimeoutException ex) {
        return response(HttpStatus.GATEWAY_TIMEOUT, ex);
    }

    @ExceptionHandler(HeavyRequestRejectedException.class)
    public ResponseEntity<ErrorResponse> handleRejected(HeavyRequestRejectedException ex) {
        return response(HttpStatus.TOO_MANY_REQUESTS, ex);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, ex);
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, Exception ex) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                MDC.get("traceId"),
                MDC.get("mode")
        ));
    }
}
