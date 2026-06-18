package com.sol.ecopulse.service.telemetry;

import com.sol.ecopulse.domain.telemetry.Telemetry;
import com.sol.ecopulse.dto.TelemetryRequest;
import com.sol.ecopulse.repository.telemetry.TelemetryRepository;
import com.sol.ecopulse.service.sensor.SensorService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TelemetryService {

    private final TelemetryRepository telemetryRepository;
    private final SensorService sensorService; // New field for SensorService
    // Shared factory for WGS84 geometry objects.
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public TelemetryService(TelemetryRepository telemetryRepository, SensorService sensorService) {
        this.telemetryRepository = telemetryRepository;
        this.sensorService = sensorService; // Initialize new SensorService
    }

    // Save telemetry with an optional PostGIS location.
    @Transactional
    public Telemetry saveTelemetry(TelemetryRequest request) {
        sensorService.getSensorOrThrow(request.sensorId()); // New validation
        LocalDateTime requestTime = request.timestamp() != null ? request.timestamp() : LocalDateTime.now();

        // Build a JTS point from the incoming coordinates. Longitude is X, latitude is Y.
        Point point = null;
        if (request.latitude() != null && request.longitude() != null) {
            point = geometryFactory.createPoint(new Coordinate(request.longitude(), request.latitude()));
        }

        Telemetry telemetry = Telemetry.builder()
                .sensorId(request.sensorId())
                .value(request.value())
                .timestamp(requestTime)
                .location(point)
                .build();

        return telemetryRepository.save(telemetry);
    }

    // Fetch telemetry history for a sensor, ordered from newest to oldest.
    public List<Telemetry> getTelemetryHistory(Long sensorId) {
        return telemetryRepository.findBySensorIdOrderByTimestampDesc(sensorId);
    }

    /**
     * Find telemetry records within a radius from the given coordinates.
     */
    public List<Telemetry> getTelemetriesNearby(double latitude, double longitude, double distanceInMeters) {
        // Create the center point using WGS84 coordinates. Longitude is X, latitude is Y.
        Point centerPoint = geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(longitude, latitude));

        return telemetryRepository.findNearbyTelemetries(centerPoint, distanceInMeters);
    }
}