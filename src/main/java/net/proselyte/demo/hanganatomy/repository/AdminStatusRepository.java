package net.proselyte.demo.hanganatomy.repository;

import java.time.Instant;

import net.proselyte.demo.hanganatomy.entity.AdminStatusRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminStatusRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminStatusRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AdminStatusRecord loadCurrent() {
        return jdbcTemplate.queryForObject("""
                        select id, "status" as status, updated_at
                        from admin_status
                        where id = 1
                        """,
                (rs, rowNum) -> new AdminStatusRecord(
                        rs.getLong("id"),
                        rs.getString("status"),
                        rs.getTimestamp("updated_at").toInstant()
                )
        );
    }

    public Instant ping() {
        return jdbcTemplate.queryForObject("select now()", (rs, rowNum) -> rs.getTimestamp(1).toInstant());
    }
}
