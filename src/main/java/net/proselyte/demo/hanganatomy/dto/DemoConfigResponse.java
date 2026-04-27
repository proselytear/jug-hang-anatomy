package net.proselyte.demo.hanganatomy.dto;

import java.util.List;
import java.util.Map;

public record DemoConfigResponse(
        String mode,
        List<String> sampleDashboardIds,
        Map<String, Object> features
) {
}
