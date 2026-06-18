package com.sol.ecopulse.controller.sensor;

import com.sol.ecopulse.domain.sensor.Sensor;
import com.sol.ecopulse.dto.SensorCreateRequest;
import com.sol.ecopulse.dto.SensorResponse; // DTO import
import com.sol.ecopulse.service.sensor.SensorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sensors")
public class SensorController {

    private final SensorService sensorService;

    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    // 1. 센서 등록 API (DTO 반환)
    @PostMapping
    public ResponseEntity<SensorResponse> createSensor(@Valid @RequestBody SensorCreateRequest request) {
        Sensor savedSensor = sensorService.saveSensor(
                request.name(),
                request.type(),
                request.latitude(),
                request.longitude()
        );

        // 엔티티 대신 DTO로 변환하여 안전하게 반환
        return ResponseEntity.ok(SensorResponse.from(savedSensor));
    }

    // 2. 주변 센서 조회 API (DTO 리스트 반환)
    @GetMapping("/nearby")
    public ResponseEntity<List<SensorResponse>> getNearbySensors(
            @RequestParam
            @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
            @DecimalMax(value = "90.0", message = "위도는 90 이하이어야 합니다.")
            double latitude,

            @RequestParam
            @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
            @DecimalMax(value = "180.0", message = "경도는 180 이하이어야 합니다.")
            double longitude,

            @RequestParam(defaultValue = "5.0")
            @Positive(message = "반경은 양수여야 합니다.")
            double radiusKm
    ) {
        List<Sensor> sensors = sensorService.getNearbySensors(latitude, longitude, radiusKm);

        // 조회된 엔티티 리스트를 모두 DTO 리스트로 변환
        List<SensorResponse> response = sensors.stream()
                .map(SensorResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}