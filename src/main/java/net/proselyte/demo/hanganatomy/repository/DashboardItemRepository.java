package net.proselyte.demo.hanganatomy.repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.proselyte.demo.hanganatomy.entity.DashboardItemRecord;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardItemRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<DashboardItemRecord> rowMapper = (rs, rowNum) -> new DashboardItemRecord(
            rs.getObject("id", UUID.class),
            rs.getObject("dashboard_id", UUID.class),
            rs.getString("metric_name"),
            rs.getBigDecimal("metric_value"),
            rs.getTimestamp("created_at").toInstant()
    );

    public DashboardItemRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DashboardItemRecord> findByDashboardId(UUID dashboardId) {
        return jdbcTemplate.query("""
                        select id, dashboard_id, metric_name, metric_value, created_at
                        from dashboard_item
                        where dashboard_id = :dashboardId
                        order by created_at
                        """,
                Map.of("dashboardId", dashboardId),
                rowMapper
        );
    }
}
