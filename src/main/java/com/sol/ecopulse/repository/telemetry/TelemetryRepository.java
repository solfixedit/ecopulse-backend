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

    // Uses the sensor_id and timestamp index to return the latest readings first.
    List<Telemetry> findBySensorIdOrderByTimestampDesc(Long sensorId);
    /**
     * Finds telemetry records within the given radius using PostGIS.
     *
     * ST_DistanceSphere calculates the distance between two points in meters,
     * taking the Earth's curvature into account.
     */
    @Query(value = "SELECT * FROM telemetries t " +
            "WHERE ST_DistanceSphere(t.location, :center) <= :distanceInMeters " +
            "ORDER BY t.timestamp DESC",
            nativeQuery = true)
    List<Telemetry> findNearbyTelemetries(@Param("center") Point center,
                                          @Param("distanceInMeters") double distanceInMeters);
}
