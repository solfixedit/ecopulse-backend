package com.sol.ecopulse.dto;

import com.sol.ecopulse.domain.sensor.Sensor;

public record SensorResponse(
        Long id,
        String name,
        String type,
        double latitude,
        double longitude
) {
    // Sensor 엔티티를 받아 DTO로 안전하게 변환해 주는 정적 팩토리 메서드
    public static SensorResponse from(Sensor sensor) {
        return new SensorResponse(
                sensor.getId(),
                sensor.getName(),
                sensor.getType(),
                sensor.getLocation().getY(), // JTS Point에서 위도(Latitude) 추출
                sensor.getLocation().getX()  // JTS Point에서 경도(Longitude) 추출
        );
    }
}