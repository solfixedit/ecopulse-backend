package com.sol.ecopulse.config;

import com.sol.ecopulse.domain.sensor.Sensor;
import com.sol.ecopulse.dto.TelemetryRequest;
import com.sol.ecopulse.service.sensor.SensorService;
import com.sol.ecopulse.service.telemetry.TelemetryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Random;

@Component
@Profile("!test") // 테스트 환경이 아닐 때만 실행되도록 안전장치 설정
public class DataInitializer implements CommandLineRunner {

    private final SensorService sensorService;
    private final TelemetryService telemetryService;
    private final Random random = new Random();

    public DataInitializer(SensorService sensorService, TelemetryService telemetryService) {
        this.sensorService = sensorService;
        this.telemetryService = telemetryService;
    }

    @Override
    public void run(String... args) throws Exception {
                // Seed a small set of sample sensors each time the app starts.
        // 여기서는 깔끔하게 서버 켤 때마다 대표 거점 3곳을 생성하는 예시입니다.

                System.out.println("====== [DataInitializer] Starting sample data seed ======");

        // 1. 서울 주요 거점 센서 등록
        // 강남역 주변 센서
        Sensor s1 = sensorService.saveSensor("강남_미세먼지_01", "FINE_DUST", 37.4979, 127.0276);
        generateFakeTelemetries(s1.getId(), 20.0, 80.0); // 20~80 사이의 랜덤 측정값 생성

        // 홍대입구역 주변 센서
        Sensor s2 = sensorService.saveSensor("홍대_온습도_01", "TEMPERATURE", 37.5567, 126.9237);
        generateFakeTelemetries(s2.getId(), 15.0, 30.0);

        // 서울시청 주변 센서
        Sensor s3 = sensorService.saveSensor("시청_소음센서_01", "NOISE", 37.5665, 126.9780);
        generateFakeTelemetries(s3.getId(), 40.0, 90.0);

                System.out.println("====== [DataInitializer] Sample data seed completed ======");
    }

        // Generate five time-series telemetry records for the given sensor.
    private void generateFakeTelemetries(Long sensorId, double minVal, double maxVal) {
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 5; i++) {
            // minVal ~ maxVal 사이의 랜덤한 실수값 생성
            double randomValue = minVal + (maxVal - minVal) * random.nextDouble();
                        // Keep one decimal place.
            randomValue = Math.round(randomValue * 10.0) / 10.0;

            // 1시간 간격으로 과거 데이터가 쌓이도록 타임스탬프 조절
            LocalDateTime timestamp = now.minusHours(i);

            TelemetryRequest request = new TelemetryRequest(sensorId, randomValue, timestamp, 43.76, -79.41);
            telemetryService.saveTelemetry(request);
        }
    }
}