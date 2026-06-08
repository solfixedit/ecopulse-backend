package com.sol.ecopulse.service.sensor;

import com.sol.ecopulse.domain.sensor.Sensor;
import com.sol.ecopulse.repository.sensor.SensorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorService {

    private final SensorRepository sensorRepository;

    @Transactional(readOnly = true)
    public List<Sensor> getSensorsNearby(double lng, double lat, double radiusInKm) {
        double radiusInMeters = radiusInKm * 1000.0; // 킬로미터를 미터 단위로 변환
        log.info("Searching sensors within {}km radius from Point({}, {})", radiusInKm, lng, lat);

        long startTime = System.currentTimeMillis();
        List<Sensor> sensors = sensorRepository.findSensorsWithinRadius(lng, lat, radiusInMeters);
        long endTime = System.currentTimeMillis();

        log.info("Found {} sensors nearby in {} ms", sensors.size(), (endTime - startTime));
        return sensors;
    }
}