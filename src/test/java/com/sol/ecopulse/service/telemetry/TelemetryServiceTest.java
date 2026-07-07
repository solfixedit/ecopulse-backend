package com.sol.ecopulse.service.telemetry;

import com.sol.ecopulse.domain.telemetry.Telemetry;
import com.sol.ecopulse.dto.TelemetryRequest;
import com.sol.ecopulse.exception.NotFoundException;
import com.sol.ecopulse.repository.telemetry.TelemetryRepository;
import com.sol.ecopulse.service.sensor.SensorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    @Mock
    private TelemetryRepository telemetryRepository;

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
    @DisplayName("getTelemetryHistory: 레포지토리의 최신순 조회 결과를 그대로 반환한다")
    void getTelemetryHistory_delegatesToRepository() {
        List<Telemetry> history = List.of(Telemetry.builder().sensorId(1L).build());
        given(telemetryRepository.findBySensorIdOrderByTimestampDesc(1L)).willReturn(history);

        List<Telemetry> result = telemetryService.getTelemetryHistory(1L);

        assertThat(result).isSameAs(history);
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
