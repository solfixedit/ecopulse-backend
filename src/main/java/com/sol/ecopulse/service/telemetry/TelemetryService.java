package com.sol.ecopulse.service.telemetry;

import com.sol.ecopulse.domain.telemetry.Telemetry;
import com.sol.ecopulse.dto.TelemetryRequest;
import com.sol.ecopulse.repository.telemetry.TelemetryRepository;
import org.locationtech.jts.geom.Coordinate; // 🎯 추가
import org.locationtech.jts.geom.GeometryFactory; // 🎯 추가
import org.locationtech.jts.geom.Point; // 🎯 추가
import org.locationtech.jts.geom.PrecisionModel; // 🎯 추가
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TelemetryService {

    private final TelemetryRepository telemetryRepository;
    // 🎯 공간 데이터 인스턴스를 일관되게 찍어낼 싱글톤 패턴 스타일의 팩토리 정의 (WGS84: 4326)
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public TelemetryService(TelemetryRepository telemetryRepository) {
        this.telemetryRepository = telemetryRepository;
    }

    // 측정 데이터 저장 (PostGIS Point 결합 가공 추가)
    @Transactional
    public Telemetry saveTelemetry(TelemetryRequest request) {
        LocalDateTime requestTime = request.timestamp() != null ? request.timestamp() : LocalDateTime.now();

        // 🎯 1. 파이썬이 던진 위경도로 JTS Point 객체 조립 (순서 주의: Longitude가 X, Latitude가 Y)
        Point point = null;
        if (request.latitude() != null && request.longitude() != null) {
            point = geometryFactory.createPoint(new Coordinate(request.longitude(), request.latitude()));
        }

        // 🎯 2. 빌더 패턴에 location 데이터 주입
        Telemetry telemetry = Telemetry.builder()
                .sensorId(request.sensorId())
                .value(request.value())
                .timestamp(requestTime)
                .location(point) // 👈 추가된 PostGIS 필드
                .build();

        return telemetryRepository.save(telemetry);
    }

    // 특정 센서의 히스토리 조회
    public List<Telemetry> getTelemetryHistory(Long sensorId) {
        return telemetryRepository.findBySensorIdOrderByTimestampDesc(sensorId);
    }
}