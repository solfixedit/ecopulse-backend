package com.sol.ecopulse.domain.telemetry;

import com.sol.ecopulse.domain.telemetry.Telemetry;
import com.sol.ecopulse.repository.telemetry.TelemetryBulkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TelemetryBulkRepositoryImpl implements TelemetryBulkRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void saveAllInBulk(List<Telemetry> telemetries) {
        String sql = "INSERT INTO telemetries (sensor_id, value, timestamp) VALUES (?, ?, ?)";

        // Batch inserts reduce database round trips for large telemetry payloads.
        jdbcTemplate.batchUpdate(sql,
                telemetries,
                1000, // Process records in chunks of 1,000.
                (PreparedStatement ps, Telemetry telemetry) -> {
                    ps.setLong(1, telemetry.getSensorId());
                    ps.setDouble(2, telemetry.getValue());
                    ps.setTimestamp(3, Timestamp.valueOf(telemetry.getTimestamp()));
                });
    }
}