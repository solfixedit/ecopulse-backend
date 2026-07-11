package com.sol.ecopulse.service.telemetry;

import com.sol.ecopulse.domain.telemetry.Telemetry;
import com.sol.ecopulse.dto.TelemetryRequest;
import com.sol.ecopulse.dto.TelemetryStatsResponse;
import com.sol.ecopulse.repository.telemetry.TelemetryBulkRepository;
import com.sol.ecopulse.repository.telemetry.TelemetryRepository;
import com.sol.ecopulse.repository.telemetry.TelemetryStats;
import com.sol.ecopulse.service.sensor.SensorService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TelemetryService {

    private final TelemetryRepository telemetryRepository;
    private final TelemetryBulkRepository telemetryBulkRepository; // High-throughput batch inserts
    private final SensorService sensorService; // New field for SensorService
    // Shared factory for WGS84 geometry objects.
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public TelemetryService(TelemetryRepository telemetryRepository,
                            TelemetryBulkRepository telemetryBulkRepository,
                            SensorService sensorService) {
        this.telemetryRepository = telemetryRepository;
        this.telemetryBulkRepository = telemetryBulkRepository;
        this.sensorService = sensorService; // Initialize new SensorService
    }

    // Save telemetry with an optional PostGIS location.
    @Transactional
    public Telemetry saveTelemetry(TelemetryRequest request) {
        sensorService.getSensorOrThrow(request.sensorId()); // New validation
        return telemetryRepository.save(toTelemetry(request));
    }

    /**
     * Ingest many telemetry readings in a single batched insert.
     *
     * <p>Unlike {@link #saveTelemetry}, per-row sensor existence is intentionally not
     * validated here — the whole point of the bulk path is to minimise database
     * round-trips for high-frequency streams.
     */
    @Transactional
    public void saveTelemetriesInBulk(List<TelemetryRequest> requests) {
        List<Telemetry> telemetries = requests.stream()
                .map(this::toTelemetry)
                .toList();
        telemetryBulkRepository.saveAllInBulk(telemetries);
    }

    // Map a request to an entity, defaulting the timestamp and building the PostGIS point
    // (longitude is X, latitude is Y) when coordinates are present.
    private Telemetry toTelemetry(TelemetryRequest request) {
        LocalDateTime requestTime = request.timestamp() != null ? request.timestamp() : LocalDateTime.now();

        Point point = null;
        if (request.latitude() != null && request.longitude() != null) {
            point = geometryFactory.createPoint(new Coordinate(request.longitude(), request.latitude()));
        }

        return Telemetry.builder()
                .sensorId(request.sensorId())
                .value(request.value())
                .timestamp(requestTime)
                .location(point)
                .build();
    }

    // Fetch a page of telemetry history for a sensor, ordered from newest to oldest.
    public Page<Telemetry> getTelemetryHistory(Long sensorId, Pageable pageable) {
        return telemetryRepository.findBySensorIdOrderByTimestampDesc(sensorId, pageable);
    }

    // Aggregate a sensor's readings (count/avg/min/max) over a [from, to] window.
    public TelemetryStatsResponse getTelemetryStats(Long sensorId, LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("조회 시작 시각(from)은 종료 시각(to)보다 이후일 수 없습니다.");
        }
        sensorService.getSensorOrThrow(sensorId);

        TelemetryStats stats = telemetryRepository.aggregateStats(sensorId, from, to);
        return TelemetryStatsResponse.of(sensorId, from, to, stats);
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