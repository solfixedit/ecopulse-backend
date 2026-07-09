package com.sol.ecopulse.repository;

import com.sol.ecopulse.domain.sensor.Sensor;
import com.sol.ecopulse.repository.sensor.SensorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SensorRepositoryIntegrationTest extends AbstractPostgisIntegrationTest {

    // WGS84 factory — mirrors the production services (X = longitude, Y = latitude).
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private SensorRepository sensorRepository;

    @Test
    @DisplayName("findNearbySensors: 반경 내 센서만 반환하고 반경 밖 센서는 제외한다")
    void findNearbySensors_returnsOnlyWithinRadius() {
        sensorRepository.save(sensorAt("시청_근처", 37.5680, 126.9800));   // 서울시청에서 약 250m
        sensorRepository.save(sensorAt("부산_먼곳", 35.1796, 129.0756));   // 약 325km
        sensorRepository.flush();

        Point center = point(37.5665, 126.9780); // 서울시청
        List<Sensor> result = sensorRepository.findNearbySensors(center, 5000.0); // 반경 5km

        assertThat(result)
                .extracting(Sensor::getName)
                .containsExactly("시청_근처");
    }

    private Sensor sensorAt(String name, double latitude, double longitude) {
        Sensor sensor = new Sensor();
        sensor.setName(name);
        sensor.setType("AIR");
        sensor.setLocation(point(latitude, longitude));
        return sensor;
    }

    private Point point(double latitude, double longitude) {
        return geometryFactory.createPoint(new Coordinate(longitude, latitude)); // X=경도, Y=위도
    }
}
