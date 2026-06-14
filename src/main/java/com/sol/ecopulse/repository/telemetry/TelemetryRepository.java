package com.sol.ecopulse.repository.telemetry;

import com.sol.ecopulse.domain.telemetry.Telemetry;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {

    // 복합 인덱스(sensor_id, timestamp)를 타서 최신 측정값부터 아주 빠르게 정렬해오는 메서드
    List<Telemetry> findBySensorIdOrderByTimestampDesc(Long sensorId);
    /**
     * 🎯 PostGIS 반경 기반 공간 쿼리
     * ST_DistanceSphere: 두 지점 간의 대원 거리(지구 곡률 반영)를 미터(m) 단위로 계산합니다.
     * 엔티티의 location과 입력받은 중심점(center) 간의 거리가 distanceInMeters 이하인 데이터만 필터링합니다.
     */
    @Query(value = "SELECT * FROM telemetries t " +
            "WHERE ST_DistanceSphere(t.location, :center) <= :distanceInMeters " +
            "ORDER BY t.timestamp DESC",
            nativeQuery = true)
    List<Telemetry> findNearbyTelemetries(@Param("center") Point center,
                                          @Param("distanceInMeters") double distanceInMeters);
}
