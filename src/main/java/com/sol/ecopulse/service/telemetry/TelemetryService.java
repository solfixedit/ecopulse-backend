package com.sol.ecopulse.service.telemetry;

import com.sol.ecopulse.domain.telemetry.Telemetry;
import com.sol.ecopulse.repository.telemetry.TelemetryBulkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryService {

    private final TelemetryBulkRepository telemetryBulkRepository;
    private final Random random = new Random();

    /**
     * 가상의 IoT 센서 데이터 대용량 생성 및 Bulk Insert 테스트 로직
     * @param count 생성할 데이터 개수 (예: 100,000건)
     */
    public void simulateAndIngestSensorData(int count) {
        long startTime = System.currentTimeMillis();
        log.info("Starting bulk ingestion for {} telemetry records...", count);

        List<Telemetry> telemetryList = new ArrayList<>(count);
        LocalDateTime now = LocalDateTime.now();

        // 시니어다운 고속 객체 생성 및 가상 데이터 세팅
        for (int i = 0; i < count; i++) {
            Telemetry telemetry = Telemetry.builder()
                    .sensorId((long) (random.nextInt(100) + 1)) // 1~100번 가상 센서 ID
                    .value(10.0 + (random.nextDouble() * 30.0)) // 10.0 ~ 40.0 사이의 환경 측정값
                    .timestamp(now.minusSeconds(i)) // 시계열 조회를 위해 과거 시간으로 역산
                    .build();
            telemetryList.add(telemetry);
        }

        // Bulk Insert 실행
        telemetryBulkRepository.saveAllInBulk(telemetryList);

        long endTime = System.currentTimeMillis();
        log.info("Successfully ingested {} records in {} ms!", count, (endTime - startTime));
    }
}