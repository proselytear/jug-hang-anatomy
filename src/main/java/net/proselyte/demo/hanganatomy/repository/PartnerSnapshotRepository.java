package net.proselyte.demo.hanganatomy.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.List;

import net.proselyte.demo.hanganatomy.client.PartnerPayload;
import net.proselyte.demo.hanganatomy.entity.PartnerSnapshotRecord;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Repository
public class PartnerSnapshotRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RowMapper<PartnerSnapshotRecord> rowMapper = (rs, rowNum) -> new PartnerSnapshotRecord(
            rs.getObject("id", UUID.class),
            rs.getObject("dashboard_id", UUID.class),
            rs.getString("external_ref"),
            rs.getString("payload"),
            rs.getTimestamp("created_at").toInstant()
    );

    public PartnerSnapshotRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<PartnerSnapshotRecord> findLatestByDashboardId(UUID dashboardId) {
        List<PartnerSnapshotRecord> rows = jdbcTemplate.query("""
                        select id, dashboard_id, external_ref, payload::text as payload, created_at
                        from partner_snapshot
                        where dashboard_id = :dashboardId
                        order by created_at desc
                        limit 1
                        """,
                Map.of("dashboardId", dashboardId),
                rowMapper
        );
        return rows.stream().findFirst();
    }

    public void saveSnapshot(UUID dashboardId, PartnerPayload payload) {
        jdbcTemplate.update("""
                        insert into partner_snapshot (id, dashboard_id, external_ref, payload, created_at)
                        values (:id, :dashboardId, :externalRef, cast(:payload as jsonb), now())
                        """,
                Map.of(
                        "id", UUID.randomUUID(),
                        "dashboardId", dashboardId,
                        "externalRef", payload.externalRef(),
                        "payload", serializePayload(payload)
                )
        );
    }

    private String serializePayload(PartnerPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload.payload());
        } catch (JacksonException ex) {
            throw new IllegalStateException("Failed to serialize partner payload", ex);
        }
    }
}
