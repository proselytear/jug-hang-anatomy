package net.proselyte.demo.hanganatomy.service;

import java.util.List;

import net.proselyte.demo.hanganatomy.entity.DashboardItemRecord;
import net.proselyte.demo.hanganatomy.entity.DashboardRecord;

public record DashboardData(
        DashboardRecord dashboard,
        List<DashboardItemRecord> items
) {
}
