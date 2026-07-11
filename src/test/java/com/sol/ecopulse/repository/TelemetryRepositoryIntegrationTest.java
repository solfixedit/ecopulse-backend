package com.sol.ecopulse.repository;

import com.sol.ecopulse.domain.telemetry.Telemetry;
import com.sol.ecopulse.repository.telemetry.TelemetryBulkRepository;
import com.sol.ecopulse.repository.telemetry.TelemetryRepository;
import com.sol.ecopulse.repository.telemetry.TelemetryStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TelemetryRepositoryIntegrationTest extends AbstractPostgisIntegrationTest {

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private TelemetryRepository telemetryRepository;

    @Autowired
    private TelemetryBulkRepository telemetryBulkRepository;

    @Test
    @DisplayName("findBySensorIdOrderByTimestampDesc: 해당 센서의 측정값을 최신순으로 반환한다")
    void findBySensorId_ordersByTimestampDesc() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        telemetryRepository.save(telemetry(1L, 10.0, base.minusHours(2), null));
        telemetryRepository.save(telemetry(1L, 20.0, base, null));             // 최신
        telemetryRepository.save(telemetry(1L, 15.0, base.minusHours(1), null));
        telemetryRepository.save(telemetry(2L, 99.0, base, null));             // 다른 센서 — 제외돼야 함
        telemetryRepository.flush();

        Page<Telemetry> result = telemetryRepository.findBySensorIdOrderByTimestampDesc(1L, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent())
                .extracting(Telemetry::getValue)
                .containsExactly(20.0, 15.0, 10.0);
    }

    @Test
    @DisplayName("findBySensorIdOrderByTimestampDesc: 페이지 크기만큼만 최신순으로 잘라 반환한다")
    void findBySensorId_paginatesNewestFirst() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        for (int i = 0; i < 5; i++) {
            telemetryRepository.save(telemetry(1L, (double) i, base.plusHours(i), null)); // i시간 뒤 = 값 i
        }
        telemetryRepository.flush();

        Page<Telemetry> firstPage = telemetryRepository.findBySensorIdOrderByTimestampDesc(1L, PageRequest.of(0, 2));

        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
        assertThat(firstPage.isLast()).isFalse();
        assertThat(firstPage.getContent())
                .extracting(Telemetry::getValue)
                .containsExactly(4.0, 3.0); // 최신 2건
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

    @Test
    @DisplayName("saveAllInBulk: 좌표를 포함해 배치 저장되어 반경 검색으로 조회된다")
    void saveAllInBulk_persistsLocation_andIsFoundByNearby() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        telemetryBulkRepository.saveAllInBulk(List.of(
                telemetry(7L, 100.0, base, point(37.5670, 126.9790)), // 서울시청 근처
                telemetry(7L, 200.0, base, point(35.1796, 129.0756))  // 부산 — 반경 밖
        ));

        Point center = point(37.5665, 126.9780); // 서울시청
        List<Telemetry> result = telemetryRepository.findNearbyTelemetries(center, 5000.0); // 반경 5km

        // 벌크 삽입 행에도 location이 채워졌고, 반경 내 1건만 조회됨을 확인
        assertThat(result)
                .extracting(Telemetry::getValue)
                .containsExactly(100.0);
    }

    @Test
    @DisplayName("aggregateStats: 기간·센서로 필터해 count/avg/min/max를 집계한다")
    void aggregateStats_computesOverWindowAndSensor() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        telemetryRepository.save(telemetry(1L, 10.0, base.plusHours(1), null));
        telemetryRepository.save(telemetry(1L, 30.0, base.plusHours(2), null));
        telemetryRepository.save(telemetry(1L, 20.0, base.plusHours(3), null));
        telemetryRepository.save(telemetry(1L, 999.0, base.plusHours(10), null)); // 기간 밖 — 제외
        telemetryRepository.save(telemetry(2L, 5.0, base.plusHours(1), null));     // 다른 센서 — 제외
        telemetryRepository.flush();

        TelemetryStats stats = telemetryRepository.aggregateStats(1L, base, base.plusHours(5));

        assertThat(stats.getTotal()).isEqualTo(3);
        assertThat(stats.getAverage()).isEqualTo(20.0); // (10+30+20)/3
        assertThat(stats.getMinimum()).isEqualTo(10.0);
        assertThat(stats.getMaximum()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("aggregateStats: 기간 내 데이터가 없으면 count는 0, avg/min/max는 null")
    void aggregateStats_emptyWindow_returnsZeroAndNulls() {
        TelemetryStats stats = telemetryRepository.aggregateStats(
                1L,
                LocalDateTime.of(2030, 1, 1, 0, 0),
                LocalDateTime.of(2030, 1, 2, 0, 0));

        assertThat(stats.getTotal()).isZero();
        assertThat(stats.getAverage()).isNull();
        assertThat(stats.getMinimum()).isNull();
        assertThat(stats.getMaximum()).isNull();
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
