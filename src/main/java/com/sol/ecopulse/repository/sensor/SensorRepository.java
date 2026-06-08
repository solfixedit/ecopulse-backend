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

    // 현재 위치(location) 기준 반경 N미터(distanceMeter) 이내에 있는 센서들을 조회하는 쿼리
    @Query(value = "SELECT * FROM sensors s WHERE ST_DistanceSphere(s.location, :location) <= :distanceMeter", nativeQuery = true)
    List<Sensor> findNearbySensors(@Param("location") Point location, @Param("distanceMeter") double distanceMeter);
}