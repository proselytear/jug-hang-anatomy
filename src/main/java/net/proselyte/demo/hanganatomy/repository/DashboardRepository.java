package net.proselyte.demo.hanganatomy.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.proselyte.demo.hanganatomy.entity.DashboardRecord;
import net.proselyte.demo.hanganatomy.service.DashboardNotFoundException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<DashboardRecord> rowMapper = (rs, rowNum) -> new DashboardRecord(
            rs.getObject("id", UUID.class),
            rs.getObject("customer_id", UUID.class),
            rs.getString("title"),
            rs.getString("status"),
            rs.getTimestamp("created_at").toInstant()
    );

    public DashboardRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardRecord findByIdRequired(UUID id) {
        List<DashboardRecord> rows = jdbcTemplate.query("""
                        select id, customer_id, title, "status" as status, created_at
                        from dashboard
                        where id = :id
                        """,
                Map.of("id", id),
                rowMapper
        );
        return rows.stream().findFirst().orElseThrow(() -> new DashboardNotFoundException("Dashboard not found: " + id));
    }

    public List<UUID> sampleIds(int limit) {
        return jdbcTemplate.query("""
                        select id
                        from dashboard
                        order by created_at desc
                        limit :limit
                        """,
                Map.of("limit", limit),
                (rs, rowNum) -> rs.getObject("id", UUID.class)
        );
    }
}
