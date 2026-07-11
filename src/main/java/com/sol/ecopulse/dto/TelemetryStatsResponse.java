package com.sol.ecopulse.dto;

import com.sol.ecopulse.repository.telemetry.TelemetryStats;

import java.time.LocalDateTime;

/**
 * Aggregate telemetry statistics for a sensor over a [from, to] window.
 * {@code average}/{@code minimum}/{@code maximum} are null when no readings match.
 */
public record TelemetryStatsResponse(
        Long sensorId,
        LocalDateTime from,
        LocalDateTime to,
        long count,
        Double average,
        Double minimum,
        Double maximum
) {
    public static TelemetryStatsResponse of(Long sensorId, LocalDateTime from, LocalDateTime to, TelemetryStats stats) {
        return new TelemetryStatsResponse(
                sensorId,
                from,
                to,
                stats.getTotal(),
                stats.getAverage(),
                stats.getMinimum(),
                stats.getMaximum()
        );
    }
}
