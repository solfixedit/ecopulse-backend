package com.sol.ecopulse.repository.telemetry;

/**
 * Aggregate statistics for a sensor's telemetry over a time window.
 *
 * <p>Spring Data interface projection. Getter names map to the (lowercase,
 * single-word) column aliases produced by the aggregate query so the mapping is
 * stable regardless of how the driver cases result-set labels.
 */
public interface TelemetryStats {

    long getTotal();      // COUNT(*)

    Double getAverage();  // AVG(value)  — null when no rows match

    Double getMinimum();  // MIN(value)  — null when no rows match

    Double getMaximum();  // MAX(value)  — null when no rows match
}
