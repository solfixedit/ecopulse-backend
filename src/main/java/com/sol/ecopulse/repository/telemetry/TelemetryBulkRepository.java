package com.sol.ecopulse.repository.telemetry;

import com.sol.ecopulse.domain.telemetry.Telemetry;
import java.util.List;

public interface TelemetryBulkRepository {
    void saveAllInBulk(List<Telemetry> telemetries);
}