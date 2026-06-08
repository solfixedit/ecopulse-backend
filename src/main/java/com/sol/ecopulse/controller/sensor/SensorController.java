package com.sol.ecopulse.controller.sensor;

import com.sol.ecopulse.domain.sensor.Sensor;
import com.sol.ecopulse.service.sensor.SensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sensors")
@RequiredArgsConstructor
public class SensorController {

    private final SensorService sensorService;

    @GetMapping("/nearby")
    public ResponseEntity<List<Sensor>> getNearbySensors(
            @RequestParam double lng,
            @RequestParam double lat,
            @RequestParam(defaultValue = "5.0") double radiusKm) {

        List<Sensor> sensors = sensorService.getSensorsNearby(lng, lat, radiusKm);
        return ResponseEntity.ok(sensors);
    }
}