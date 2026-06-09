package com.sol.ecopulse.dto;

import com.sol.ecopulse.domain.telemetry.Telemetry;
import java.time.LocalDateTime;

public record TelemetryResponse(
        Long id,
        Long sensorId,
        Double value,
        LocalDateTime timestamp
) {
    public static TelemetryResponse from(Telemetry telemetry) {
        return new TelemetryResponse(
                telemetry.getId(),
                telemetry.getSensorId(),
                telemetry.getValue(),
                telemetry.getTimestamp()
        );
    }
}