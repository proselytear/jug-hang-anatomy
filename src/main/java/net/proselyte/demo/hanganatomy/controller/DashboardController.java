package net.proselyte.demo.hanganatomy.controller;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import net.proselyte.demo.hanganatomy.dto.DashboardResponse;
import net.proselyte.demo.hanganatomy.observability.RequestLoggingInterceptor;
import net.proselyte.demo.hanganatomy.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard/{id}")
    public DashboardResponse getDashboard(@PathVariable UUID id, HttpServletRequest request) {
        DashboardResponse response = dashboardService.getDashboard(id);
        request.setAttribute(RequestLoggingInterceptor.OUTCOME_ATTRIBUTE, response.outcome());
        return response;
    }
}
