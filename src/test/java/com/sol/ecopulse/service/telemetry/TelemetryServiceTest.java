package com.sol.ecopulse.service.telemetry;

import com.sol.ecopulse.domain.telemetry.Telemetry;
import com.sol.ecopulse.dto.TelemetryRequest;
import com.sol.ecopulse.dto.TelemetryStatsResponse;
import com.sol.ecopulse.exception.NotFoundException;
import com.sol.ecopulse.repository.telemetry.TelemetryBulkRepository;
import com.sol.ecopulse.repository.telemetry.TelemetryRepository;
import com.sol.ecopulse.repository.telemetry.TelemetryStats;
import com.sol.ecopulse.service.sensor.SensorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    @Mock
    private TelemetryRepository telemetryRepository;

    @Mock
    private TelemetryBulkRepository telemetryBulkRepository;

    @Mock
    private SensorService sensorService;

    @InjectMocks
    private TelemetryService telemetryService;

    @Test
    @DisplayName("saveTelemetry: 존재하지 않는 센서면 NotFoundException을 전파하고 저장하지 않는다")
    void saveTelemetry_sensorNotFound_propagatesAndDoesNotSave() {
        given(sensorService.getSensorOrThrow(99L))
                .willThrow(new NotFoundException("존재하지 않는 센서입니다. sensorId=99"));

        TelemetryRequest request = new TelemetryRequest(99L, 1.0, null, 37.5, 127.0);

        assertThatThrownBy(() -> telemetryService.saveTelemetry(request))
                .isInstanceOf(NotFoundException.class);

        verify(telemetryRepository, never()).save(any());
    }

    @Test
    @DisplayName("saveTelemetry: 좌표가 있으면 경도(X)·위도(Y) Point로 매핑하고 요청 타임스탬프를 사용한다")
    void saveTelemetry_withCoordinates_mapsPointAndTimestamp() {
        given(telemetryRepository.save(any(Telemetry.class))).willAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 1, 0, 0);
        TelemetryRequest request = new TelemetryRequest(1L, 21.5, timestamp, 37.5, 127.0);

        telemetryService.saveTelemetry(request);

        verify(sensorService).getSensorOrThrow(1L);

        ArgumentCaptor<Telemetry> captor = ArgumentCaptor.forClass(Telemetry.class);
        verify(telemetryRepository).save(captor.capture());

        Telemetry saved = captor.getValue();
        assertThat(saved.getSensorId()).isEqualTo(1L);
        assertThat(saved.getValue()).isEqualTo(21.5);
        assertThat(saved.getTimestamp()).isEqualTo(timestamp);

        Point location = saved.getLocation();
        assertThat(location).isNotNull();
        assertThat(location.getX()).isEqualTo(127.0); // X = 경도
        assertThat(location.getY()).isEqualTo(37.5);  // Y = 위도
        assertThat(location.getSRID()).isEqualTo(4326);
    }

    @Test
    @DisplayName("saveTelemetry: 타임스탬프가 없으면 현재 시각으로 채우고, 좌표가 없으면 location은 null이다")
    void saveTelemetry_withoutTimestampAndCoordinates_defaultsTimestampAndNullLocation() {
        given(telemetryRepository.save(any(Telemetry.class))).willAnswer(invocation -> invocation.getArgument(0));

        TelemetryRequest request = new TelemetryRequest(1L, 21.5, null, null, null);

        telemetryService.saveTelemetry(request);

        ArgumentCaptor<Telemetry> captor = ArgumentCaptor.forClass(Telemetry.class);
        verify(telemetryRepository).save(captor.capture());

        Telemetry saved = captor.getValue();
        assertThat(saved.getTimestamp()).isNotNull();
        assertThat(saved.getLocation()).isNull();
    }

    @Test
    @DisplayName("saveTelemetriesInBulk: 요청들을 엔티티로 매핑해 배치 저장에 위임하고, 센서 검증은 하지 않는다")
    @SuppressWarnings("unchecked")
    void saveTelemetriesInBulk_mapsAndDelegates() {
        List<TelemetryRequest> requests = List.of(
                new TelemetryRequest(1L, 10.0, LocalDateTime.of(2026, 1, 1, 0, 0), 37.5, 127.0),
                new TelemetryRequest(2L, 20.0, null, null, null) // 좌표/타임스탬프 없음
        );

        telemetryService.saveTelemetriesInBulk(requests);

        ArgumentCaptor<List<Telemetry>> captor = ArgumentCaptor.forClass(List.class);
        verify(telemetryBulkRepository).saveAllInBulk(captor.capture());

        List<Telemetry> saved = captor.getValue();
        assertThat(saved).hasSize(2);

        // 좌표가 있으면 Point(X=경도, Y=위도)로 매핑
        assertThat(saved.get(0).getLocation()).isNotNull();
        assertThat(saved.get(0).getLocation().getX()).isEqualTo(127.0);
        assertThat(saved.get(0).getLocation().getY()).isEqualTo(37.5);
        assertThat(saved.get(0).getTimestamp()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));

        // 좌표 없으면 location은 null, 타임스탬프 없으면 기본값으로 채움
        assertThat(saved.get(1).getLocation()).isNull();
        assertThat(saved.get(1).getTimestamp()).isNotNull();

        // 벌크 경로는 센서 존재 검증을 하지 않는다
        verifyNoInteractions(sensorService);
    }

    @Test
    @DisplayName("getTelemetryHistory: 페이지 요청을 그대로 전달하고 레포지토리 결과 페이지를 반환한다")
    void getTelemetryHistory_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Telemetry> history = new PageImpl<>(List.of(Telemetry.builder().sensorId(1L).build()));
        given(telemetryRepository.findBySensorIdOrderByTimestampDesc(1L, pageable)).willReturn(history);

        Page<Telemetry> result = telemetryService.getTelemetryHistory(1L, pageable);

        assertThat(result).isSameAs(history);
    }

    @Test
    @DisplayName("getTelemetryStats: 센서 검증 후 기간 집계를 응답 DTO로 매핑해 반환한다")
    void getTelemetryStats_returnsMappedStats() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 2, 0, 0);

        TelemetryStats projection = mock(TelemetryStats.class);
        given(projection.getTotal()).willReturn(3L);
        given(projection.getAverage()).willReturn(20.0);
        given(projection.getMinimum()).willReturn(10.0);
        given(projection.getMaximum()).willReturn(30.0);
        given(telemetryRepository.aggregateStats(1L, from, to)).willReturn(projection);

        TelemetryStatsResponse result = telemetryService.getTelemetryStats(1L, from, to);

        verify(sensorService).getSensorOrThrow(1L);
        assertThat(result.sensorId()).isEqualTo(1L);
        assertThat(result.from()).isEqualTo(from);
        assertThat(result.to()).isEqualTo(to);
        assertThat(result.count()).isEqualTo(3L);
        assertThat(result.average()).isEqualTo(20.0);
        assertThat(result.minimum()).isEqualTo(10.0);
        assertThat(result.maximum()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("getTelemetryStats: from이 to보다 이후면 IllegalArgumentException을 던지고 집계하지 않는다")
    void getTelemetryStats_fromAfterTo_throws() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 2, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 1, 0, 0);

        assertThatThrownBy(() -> telemetryService.getTelemetryStats(1L, from, to))
                .isInstanceOf(IllegalArgumentException.class);

        verify(telemetryRepository, never()).aggregateStats(any(), any(), any());
        verifyNoInteractions(sensorService);
    }

    @Test
    @DisplayName("getTelemetriesNearby: 중심점과 거리(m)를 만들어 레포지토리에 위임한다")
    void getTelemetriesNearby_buildsCenterAndDelegates() {
        List<Telemetry> nearby = List.of(Telemetry.builder().sensorId(1L).build());
        given(telemetryRepository.findNearbyTelemetries(any(Point.class), any(Double.class))).willReturn(nearby);

        List<Telemetry> result = telemetryService.getTelemetriesNearby(37.5, 127.0, 5000.0);

        assertThat(result).isSameAs(nearby);

        ArgumentCaptor<Point> pointCaptor = ArgumentCaptor.forClass(Point.class);
        ArgumentCaptor<Double> distanceCaptor = ArgumentCaptor.forClass(Double.class);
        verify(telemetryRepository).findNearbyTelemetries(pointCaptor.capture(), distanceCaptor.capture());

        Point center = pointCaptor.getValue();
        assertThat(center.getX()).isEqualTo(127.0); // X = 경도
        assertThat(center.getY()).isEqualTo(37.5);  // Y = 위도
        assertThat(distanceCaptor.getValue()).isEqualTo(5000.0);
    }
}
