package com.sol.ecopulse.controller.telemetry;

import com.sol.ecopulse.domain.telemetry.Telemetry;
import com.sol.ecopulse.dto.TelemetryRequest;
import com.sol.ecopulse.dto.TelemetryResponse;
import com.sol.ecopulse.service.telemetry.TelemetryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import com.sol.ecopulse.dto.PageResponse;
import com.sol.ecopulse.dto.TelemetryStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/telemetries")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    // Create a telemetry reading for a sensor.
    @PostMapping
    public ResponseEntity<TelemetryResponse> createTelemetry(@Valid @RequestBody TelemetryRequest request) {
        Telemetry savedTelemetry = telemetryService.saveTelemetry(request);
        return ResponseEntity.ok(TelemetryResponse.from(savedTelemetry));
    }

    // Bulk-ingest telemetry readings via a single batched insert (high-throughput path).
    @PostMapping("/bulk")
    public ResponseEntity<Void> createTelemetriesInBulk(
            @RequestBody @Valid List<@Valid TelemetryRequest> requests
    ) {
        telemetryService.saveTelemetriesInBulk(requests);
        return ResponseEntity.accepted().build();
    }

    // Get a page of telemetry history for a sensor (newest first). Controlled via ?page=&size=.
    @GetMapping("/sensor/{sensorId}")
    public ResponseEntity<PageResponse<TelemetryResponse>> getTelemetryBySensor(
            @PathVariable Long sensorId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<TelemetryResponse> page = telemetryService.getTelemetryHistory(sensorId, pageable)
                .map(TelemetryResponse::from);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    // Aggregate a sensor's readings over a time window: /sensor/{id}/stats?from=&to= (ISO date-time).
    @GetMapping("/sensor/{sensorId}/stats")
    public ResponseEntity<TelemetryStatsResponse> getTelemetryStats(
            @PathVariable Long sensorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ResponseEntity.ok(telemetryService.getTelemetryStats(sensorId, from, to));
    }

    /**
     * Search telemetry readings within a radius from the given coordinates.
     *
     * Example:
     * GET /api/telemetries/nearby?lat=43.756&lon=-79.417&radius=5000
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<TelemetryResponse>> getNearbyTelemetries(
            @RequestParam("lat")
            @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
            @DecimalMax(value = "90.0", message = "위도는 90 이하이어야 합니다.")
            double lat,

            @RequestParam("lon")
            @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
            @DecimalMax(value = "180.0", message = "경도는 180 이하이어야 합니다.")
            double lon,

            @RequestParam(value = "radius", defaultValue = "5000")
            @Positive(message = "반경은 양수여야 합니다.")
            double radiusInMeters
    ) {
        List<TelemetryResponse> responses = telemetryService.getTelemetriesNearby(lat, lon, radiusInMeters)
                .stream()
                .map(TelemetryResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }
}