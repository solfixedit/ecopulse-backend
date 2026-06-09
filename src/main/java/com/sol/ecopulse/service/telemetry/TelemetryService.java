package com.sol.ecopulse.service.telemetry;

import com.sol.ecopulse.domain.telemetry.Telemetry;
import com.sol.ecopulse.dto.TelemetryRequest;
import com.sol.ecopulse.repository.telemetry.TelemetryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TelemetryService {

    private final TelemetryRepository telemetryRepository;

    public TelemetryService(TelemetryRepository telemetryRepository) {
        this.telemetryRepository = telemetryRepository;
    }

    // 측정 데이터 저장 (기본 단건 저장 후 DTO 변환용 엔티티 리턴)
    @Transactional
    public Telemetry saveTelemetry(TelemetryRequest request) {
        LocalDateTime requestTime = request.timestamp() != null ? request.timestamp() : LocalDateTime.now();

        Telemetry telemetry = Telemetry.builder()
                .sensorId(request.sensorId())
                .value(request.value())
                .timestamp(requestTime)
                .build();

        return telemetryRepository.save(telemetry);
    }

    // 특정 센서의 히스토리 조회
    public List<Telemetry> getTelemetryHistory(Long sensorId) {
        return telemetryRepository.findBySensorIdOrderByTimestampDesc(sensorId);
    }
}