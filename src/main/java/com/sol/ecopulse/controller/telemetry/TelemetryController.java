package com.sol.ecopulse.controller.telemetry;

import com.sol.ecopulse.domain.telemetry.Telemetry;
import com.sol.ecopulse.dto.TelemetryRequest;
import com.sol.ecopulse.dto.TelemetryResponse;
import com.sol.ecopulse.service.telemetry.TelemetryService;
import org.springframework.http.ResponseEntity;
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

    // 1. 특정 센서의 환경 측정값 등록 API (POST /api/telemetries)
    @PostMapping
    public ResponseEntity<TelemetryResponse> createTelemetry(@RequestBody TelemetryRequest request) {
        Telemetry savedTelemetry = telemetryService.saveTelemetry(request);
        return ResponseEntity.ok(TelemetryResponse.from(savedTelemetry));
    }

    // 2. 특정 센서의 누적 데이터 조회 API (GET /api/telemetries/sensor/{sensorId})
    @GetMapping("/sensor/{sensorId}")
    public ResponseEntity<List<TelemetryResponse>> getTelemetryBySensor(@PathVariable Long sensorId) {
        List<Telemetry> telemetries = telemetryService.getTelemetryHistory(sensorId);

        List<TelemetryResponse> responseList = telemetries.stream()
                .map(TelemetryResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }
}