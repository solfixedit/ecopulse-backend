package com.sol.ecopulse.repository.telemetry;

import com.sol.ecopulse.domain.telemetry.Telemetry;
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

        // batchUpdate를 통해 10,000건의 데이터를 단 몇 번의 네트워크 통신으로 DB에 밀어 넣습니다.
        jdbcTemplate.batchUpdate(sql,
                telemetries,
                1000, // 배치 사이즈: 1000건씩 묶어서 다중 컴파일 처리
                (PreparedStatement ps, Telemetry telemetry) -> {
                    ps.setLong(1, telemetry.getSensorId());
                    ps.setDouble(2, telemetry.getValue());
                    ps.setTimestamp(3, Timestamp.valueOf(telemetry.getTimestamp()));
                });
    }
}