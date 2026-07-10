package com.sol.ecopulse.repository.sensor;

import com.sol.ecopulse.domain.sensor.Sensor; // Sol님의 엔티티 패키지 경로에 맞게 수정하세요!
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, Long> {

    // 현재 위치(location) 기준 반경 N미터(distanceMeter) 이내에 있는 센서들을 조회하는 쿼리.
    // geography로 캐스팅하면 ST_DWithin이 미터 단위 측지 거리로 동작하며,
    // (location::geography) GiST 인덱스를 사용해 전체 행 스캔을 피한다. (SpatialIndexInitializer 참고)
    @Query(value = "SELECT * FROM sensors s " +
            "WHERE ST_DWithin(CAST(s.location AS geography), CAST(:location AS geography), :distanceMeter)",
            nativeQuery = true)
    List<Sensor> findNearbySensors(@Param("location") Point location, @Param("distanceMeter") double distanceMeter);
}