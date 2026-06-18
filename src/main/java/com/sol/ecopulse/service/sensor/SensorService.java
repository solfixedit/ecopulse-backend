package com.sol.ecopulse.service.sensor;

import com.sol.ecopulse.domain.sensor.Sensor;
import com.sol.ecopulse.repository.sensor.SensorRepository;
import com.sol.ecopulse.exception.NotFoundException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SensorService {

    private final SensorRepository sensorRepository;
    // SRID 4326은 전 세계 표준 GPS 좌표계(WGS84)를 뜻합니다.
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public SensorService(SensorRepository sensorRepository) {
        this.sensorRepository = sensorRepository;
    }

    // 센서 저장하기
    public Sensor saveSensor(String name, String type, double latitude, double longitude) { // type 추가
        Sensor sensor = new Sensor();
        sensor.setName(name);
        sensor.setType(type); // 엔티티에 type 설정 (만약 필드명이 다르면 실제 필드명에 맞추세요)

        // 위도, 경도 좌표를 JTS Point 객체로 생성
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        sensor.setLocation(point);

        return sensorRepository.save(sensor);
    }

    public Sensor getSensorOrThrow(Long sensorId) {
        return sensorRepository.findById(sensorId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 센서입니다. sensorId=" + sensorId));
    }
    
    // 주변 센서 검색하기 (반경은 킬로미터(km) 단위로 입력받아 미터로 변환)
    public List<Sensor> getNearbySensors(double latitude, double longitude, double radiusKm) {
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        double distanceMeter = radiusKm * 1000; // km -> m 변환
        return sensorRepository.findNearbySensors(point, distanceMeter);
    }
}