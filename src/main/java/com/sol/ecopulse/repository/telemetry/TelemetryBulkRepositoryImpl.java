package com.sol.ecopulse.repository.telemetry;

import com.sol.ecopulse.domain.telemetry.Telemetry;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TelemetryBulkRepositoryImpl implements TelemetryBulkRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void saveAllInBulk(List<Telemetry> telemetries) {
        // Build the PostGIS point in-SQL from lon/lat so the location column is populated too.
        // ST_MakePoint(lon, lat) -> ST_SetSRID(..., 4326) yields a geometry(Point, 4326) matching the column.
        String sql = "INSERT INTO telemetries (sensor_id, value, timestamp, location) " +
                "VALUES (?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326))";

        // Batch inserts reduce database round trips for large telemetry payloads.
        jdbcTemplate.batchUpdate(sql,
                telemetries,
                1000, // Process records in chunks of 1,000.
                (PreparedStatement ps, Telemetry telemetry) -> {
                    ps.setLong(1, telemetry.getSensorId());
                    ps.setDouble(2, telemetry.getValue());
                    ps.setTimestamp(3, Timestamp.valueOf(telemetry.getTimestamp()));

                    Point location = telemetry.getLocation();
                    if (location != null) {
                        ps.setDouble(4, location.getX()); // X = longitude
                        ps.setDouble(5, location.getY()); // Y = latitude
                    } else {
                        // ST_MakePoint(NULL, NULL) -> NULL, so rows without coordinates keep a null location.
                        ps.setNull(4, Types.DOUBLE);
                        ps.setNull(5, Types.DOUBLE);
                    }
                });
    }
}
