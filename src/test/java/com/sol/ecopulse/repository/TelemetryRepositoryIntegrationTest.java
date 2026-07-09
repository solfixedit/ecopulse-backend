package com.sol.ecopulse.repository;

import com.sol.ecopulse.domain.telemetry.Telemetry;
import com.sol.ecopulse.repository.telemetry.TelemetryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TelemetryRepositoryIntegrationTest extends AbstractPostgisIntegrationTest {

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private TelemetryRepository telemetryRepository;

    @Test
    @DisplayName("findBySensorIdOrderByTimestampDesc: 해당 센서의 측정값을 최신순으로 반환한다")
    void findBySensorId_ordersByTimestampDesc() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        telemetryRepository.save(telemetry(1L, 10.0, base.minusHours(2), null));
        telemetryRepository.save(telemetry(1L, 20.0, base, null));             // 최신
        telemetryRepository.save(telemetry(1L, 15.0, base.minusHours(1), null));
        telemetryRepository.save(telemetry(2L, 99.0, base, null));             // 다른 센서 — 제외돼야 함
        telemetryRepository.flush();

        List<Telemetry> result = telemetryRepository.findBySensorIdOrderByTimestampDesc(1L);

        assertThat(result)
                .extracting(Telemetry::getValue)
                .containsExactly(20.0, 15.0, 10.0);
    }

    @Test
    @DisplayName("findNearbyTelemetries: 반경 내 측정값만 최신순으로 반환하고 반경 밖은 제외한다")
    void findNearbyTelemetries_returnsOnlyWithinRadius() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        telemetryRepository.save(telemetry(1L, 1.0, base, point(37.5680, 126.9800)));            // 약 250m
        telemetryRepository.save(telemetry(1L, 2.0, base.plusHours(1), point(37.5670, 126.9790))); // 근처, 더 최신
        telemetryRepository.save(telemetry(1L, 3.0, base, point(35.1796, 129.0756)));            // 부산 — 제외돼야 함
        telemetryRepository.flush();

        Point center = point(37.5665, 126.9780); // 서울시청
        List<Telemetry> result = telemetryRepository.findNearbyTelemetries(center, 5000.0); // 반경 5km

        assertThat(result)
                .extracting(Telemetry::getValue)
                .containsExactly(2.0, 1.0); // 둘 다 반경 내, timestamp desc; 부산은 제외
    }

    private Telemetry telemetry(Long sensorId, double value, LocalDateTime timestamp, Point location) {
        return Telemetry.builder()
                .sensorId(sensorId)
                .value(value)
                .timestamp(timestamp)
                .location(location)
                .build();
    }

    private Point point(double latitude, double longitude) {
        return geometryFactory.createPoint(new Coordinate(longitude, latitude)); // X=경도, Y=위도
    }
}
