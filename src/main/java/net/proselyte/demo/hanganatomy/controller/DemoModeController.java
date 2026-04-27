package net.proselyte.demo.hanganatomy.controller;

import net.proselyte.demo.hanganatomy.dto.DemoConfigResponse;
import net.proselyte.demo.hanganatomy.observability.DemoMode;
import net.proselyte.demo.hanganatomy.service.DemoScenarioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo")
public class DemoModeController {

    private final DemoScenarioService demoScenarioService;

    public DemoModeController(DemoScenarioService demoScenarioService) {
        this.demoScenarioService = demoScenarioService;
    }

    @PostMapping("/mode/{mode}")
    public DemoConfigResponse switchMode(@PathVariable String mode) {
        return demoScenarioService.switchMode(DemoMode.fromToken(mode));
    }

    @GetMapping("/config")
    public DemoConfigResponse currentConfig() {
        return demoScenarioService.currentConfig();
    }
}
