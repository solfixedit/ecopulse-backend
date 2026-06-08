package com.sol.ecopulse.controller.sensor;

import com.sol.ecopulse.domain.sensor.Sensor;
import com.sol.ecopulse.dto.SensorResponse; // DTO import
import com.sol.ecopulse.service.sensor.SensorService;
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
    public ResponseEntity<SensorResponse> createSensor(
            @RequestParam String name,
            @RequestParam String type,
            @RequestParam double latitude,
            @RequestParam double longitude) {
        Sensor savedSensor = sensorService.saveSensor(name, type, latitude, longitude);

        // 엔티티 대신 DTO로 변환하여 안전하게 반환
        return ResponseEntity.ok(SensorResponse.from(savedSensor));
    }

    // 2. 주변 센서 조회 API (DTO 리스트 반환)
    @GetMapping("/nearby")
    public ResponseEntity<List<SensorResponse>> getNearbySensors(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5.0") double radiusKm) {
        List<Sensor> sensors = sensorService.getNearbySensors(latitude, longitude, radiusKm);

        // 조회된 엔티티 리스트를 모두 DTO 리스트로 변환
        List<SensorResponse> response = sensors.stream()
                .map(SensorResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}