package com.sol.ecopulse.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Creates GiST spatial indexes on the geometry columns after Hibernate has created
 * the tables.
 *
 * <p>JPA's {@code @Index} can only express plain (B-tree) indexes — it has no way to
 * request an index method — so a GiST index cannot be declared on the entities. We
 * create it here idempotently ({@code IF NOT EXISTS}) via raw DDL. The index is built
 * on {@code (location::geography)} to match the {@code ST_DWithin(CAST(... AS geography), ...)}
 * predicate used by the nearby queries, allowing the planner to use it for radius search.
 *
 * <p>Runs before {@code DataInitializer} (which is {@code @Order}-less / later) so the
 * index exists before any sample data is seeded.
 */
@Component
@Order(0)
public class SpatialIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SpatialIndexInitializer.class);

    private static final List<String> INDEX_STATEMENTS = List.of(
            "CREATE INDEX IF NOT EXISTS idx_sensors_location_gix ON sensors USING gist ((location::geography))",
            "CREATE INDEX IF NOT EXISTS idx_telemetries_location_gix ON telemetries USING gist ((location::geography))"
    );

    private final JdbcTemplate jdbcTemplate;

    public SpatialIndexInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String statement : INDEX_STATEMENTS) {
            jdbcTemplate.execute(statement);
        }
        log.info("Ensured GiST spatial indexes on sensors/telemetries location columns.");
    }
}
