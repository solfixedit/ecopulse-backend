package com.sol.ecopulse.controller.telemetry;

import com.sol.ecopulse.domain.telemetry.Telemetry;
import com.sol.ecopulse.dto.TelemetryRequest;
import com.sol.ecopulse.dto.TelemetryResponse;
import com.sol.ecopulse.service.telemetry.TelemetryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/telemetries")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    // Create a telemetry reading for a sensor.
    @Validated
    @PostMapping
    public ResponseEntity<TelemetryResponse> createTelemetry(@Valid @RequestBody TelemetryRequest request) {
        Telemetry savedTelemetry = telemetryService.saveTelemetry(request);
        return ResponseEntity.ok(TelemetryResponse.from(savedTelemetry));
    }

    // Get telemetry history for a sensor.
    @GetMapping("/sensor/{sensorId}")
    public ResponseEntity<List<TelemetryResponse>> getTelemetryBySensor(@PathVariable Long sensorId) {
        List<Telemetry> telemetries = telemetryService.getTelemetryHistory(sensorId);

        List<TelemetryResponse> responseList = telemetries.stream()
                .map(TelemetryResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    /**
     * Search telemetry readings within a radius from the given coordinates.
     *
     * Example:
     * GET /api/telemetries/nearby?lat=43.756&lon=-79.417&radius=5000
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<TelemetryResponse>> getNearbyTelemetries(
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon,
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