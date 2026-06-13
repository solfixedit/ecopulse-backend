package com.sol.ecopulse.dto;

import com.sol.ecopulse.domain.telemetry.Telemetry;
import java.time.LocalDateTime;

public record TelemetryResponse(
        Long id,
        Long sensorId,
        Double value,
        LocalDateTime timestamp,
        Double latitude,   // 1. JSON 응답에 포함할 위도 추가
        Double longitude  // 2. JSON 응답에 포함할 경도 추가
) {
    public static TelemetryResponse from(Telemetry telemetry) {
        Double lat = null;
        Double lon = null;

        // 🎯 3. PostGIS Point 객체에서 좌표 추출 (X = 경도, Y = 위도)
        if (telemetry.getLocation() != null) {
            lon = telemetry.getLocation().getX();
            lat = telemetry.getLocation().getY();
        }

        return new TelemetryResponse(
                telemetry.getId(),
                telemetry.getSensorId(),
                telemetry.getValue(),
                telemetry.getTimestamp(),
                lat,  // 주입
                lon   // 주입
        );
    }
}