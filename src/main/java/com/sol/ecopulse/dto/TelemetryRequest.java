package com.sol.ecopulse.dto;

import java.time.LocalDateTime;

public record TelemetryRequest(
        Long sensorId,
        Double value,
        LocalDateTime timestamp,
        Double latitude,
        Double longitude  
) {}