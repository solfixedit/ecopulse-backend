package com.sol.ecopulse.repository.telemetry;

import com.sol.ecopulse.domain.telemetry.Telemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {

    // 복합 인덱스(sensor_id, timestamp)를 타서 최신 측정값부터 아주 빠르게 정렬해오는 메서드
    List<Telemetry> findBySensorIdOrderByTimestampDesc(Long sensorId);
}