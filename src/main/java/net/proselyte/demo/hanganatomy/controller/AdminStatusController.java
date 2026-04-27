package net.proselyte.demo.hanganatomy.controller;

import net.proselyte.demo.hanganatomy.dto.AdminStatusResponse;
import net.proselyte.demo.hanganatomy.service.AdminStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminStatusController {

    private final AdminStatusService adminStatusService;

    public AdminStatusController(AdminStatusService adminStatusService) {
        this.adminStatusService = adminStatusService;
    }

    @GetMapping("/status")
    public AdminStatusResponse getStatus() {
        return adminStatusService.getStatus();
    }
}
