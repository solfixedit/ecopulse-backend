package com.sol.ecopulse.repository.telemetry;

import com.sol.ecopulse.domain.telemetry.Telemetry;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {

    // Uses the sensor_id + timestamp composite index to return the latest readings first,
    // paginated to avoid loading an unbounded time-series into memory.
    Page<Telemetry> findBySensorIdOrderByTimestampDesc(Long sensorId, Pageable pageable);
    /**
     * Finds telemetry records within the given radius (in meters) using PostGIS.
     *
     * <p>Casting to {@code geography} makes ST_DWithin measure geodesic distance in
     * meters and, crucially, lets the planner use the GiST index on
     * {@code (location::geography)} (see SpatialIndexInitializer) instead of scanning
     * every row — unlike a {@code ST_DistanceSphere(...) <= d} predicate.
     */
    @Query(value = "SELECT * FROM telemetries t " +
            "WHERE ST_DWithin(CAST(t.location AS geography), CAST(:center AS geography), :distanceInMeters) " +
            "ORDER BY t.timestamp DESC",
            nativeQuery = true)
    List<Telemetry> findNearbyTelemetries(@Param("center") Point center,
                                          @Param("distanceInMeters") double distanceInMeters);
}
