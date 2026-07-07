package com.sol.ecopulse.service.sensor;

import com.sol.ecopulse.domain.sensor.Sensor;
import com.sol.ecopulse.exception.NotFoundException;
import com.sol.ecopulse.repository.sensor.SensorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SensorServiceTest {

    @Mock
    private SensorRepository sensorRepository;

    @InjectMocks
    private SensorService sensorService;

    @Test
    @DisplayName("getSensorOrThrow: 센서가 존재하면 해당 센서를 반환한다")
    void getSensorOrThrow_found_returnsSensor() {
        Sensor sensor = new Sensor();
        sensor.setName("air-1");
        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));

        Sensor result = sensorService.getSensorOrThrow(1L);

        assertThat(result).isSameAs(sensor);
    }

    @Test
    @DisplayName("getSensorOrThrow: 센서가 없으면 sensorId를 포함한 NotFoundException을 던진다")
    void getSensorOrThrow_notFound_throws() {
        given(sensorRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sensorService.getSensorOrThrow(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("saveSensor: 이름/타입을 설정하고 경도(X)·위도(Y) 좌표로 Point를 만들어 저장한다")
    void saveSensor_mapsFieldsAndCoordinates() {
        given(sensorRepository.save(any(Sensor.class))).willAnswer(invocation -> invocation.getArgument(0));

        double latitude = 37.5;
        double longitude = 127.0;
        sensorService.saveSensor("air-1", "AIR", latitude, longitude);

        ArgumentCaptor<Sensor> captor = ArgumentCaptor.forClass(Sensor.class);
        verify(sensorRepository).save(captor.capture());

        Sensor saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("air-1");
        assertThat(saved.getType()).isEqualTo("AIR");

        Point location = saved.getLocation();
        assertThat(location.getX()).isEqualTo(longitude); // X = 경도
        assertThat(location.getY()).isEqualTo(latitude);  // Y = 위도
        assertThat(location.getSRID()).isEqualTo(4326);
    }

    @Test
    @DisplayName("getNearbySensors: 반경(km)을 미터로 변환하여 레포지토리에 전달한다")
    void getNearbySensors_convertsKmToMeters() {
        sensorService.getNearbySensors(37.5, 127.0, 5.0);

        verify(sensorRepository).findNearbySensors(any(Point.class), eq(5000.0)); // 5km -> 5000m
    }
}
