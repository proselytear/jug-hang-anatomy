package net.proselyte.demo.hanganatomy.dto;

import java.math.BigDecimal;

public record DashboardItemResponse(
        String metricName,
        BigDecimal metricValue
) {
}
