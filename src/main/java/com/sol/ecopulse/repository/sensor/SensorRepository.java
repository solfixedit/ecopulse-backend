package com.sol.ecopulse.repository.sensor;

import com.sol.ecopulse.domain.sensor.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, Long> {

    /**
     * PostGIS의 ST_DWithin 함수를 사용하여 특정 중심점(위경도) 기준 반경 N미터 이내의 센서를 고속 조회합니다.
     * SRID 4326(WGS84 위경도 좌표계)을 기준으로 하며, 정확한 미터 단위 계산을 위해 geography 타입으로 캐스팅합니다.
     */
    @Query(value = "SELECT * FROM sensors s " +
            "WHERE ST_DWithin(s.location::geography, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusInMeters)",
            nativeQuery = true)
    List<Sensor> findSensorsWithinRadius(@Param("lng") double lng,
                                         @Param("lat") double lat,
                                         @Param("radiusInMeters") double radiusInMeters);
}