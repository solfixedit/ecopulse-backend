package com.sol.ecopulse.controller.telemetry;

import com.sol.ecopulse.domain.telemetry.Telemetry;
import com.sol.ecopulse.dto.TelemetryStatsResponse;
import com.sol.ecopulse.exception.NotFoundException;
import com.sol.ecopulse.service.telemetry.TelemetryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TelemetryController.class)
class TelemetryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TelemetryService telemetryService;

    @Test
    @DisplayName("필수 값이 누락되면 400과 INVALID_REQUEST 코드, 필드 에러 목록을 반환한다")
    void createTelemetry_validationFailure_returnsBadRequest() throws Exception {
        // value / latitude / longitude 누락
        String body = """
                { "sensorId": 1 }
                """;

        mockMvc.perform(post("/api/telemetries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("존재하지 않는 센서면 404와 NOT_FOUND 코드를 반환한다")
    void createTelemetry_sensorNotFound_returnsNotFound() throws Exception {
        given(telemetryService.saveTelemetry(any()))
                .willThrow(new NotFoundException("존재하지 않는 센서입니다. sensorId=99"));

        String body = """
                { "sensorId": 99, "value": 1.0, "latitude": 37.5, "longitude": 127.0 }
                """;

        mockMvc.perform(post("/api/telemetries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("예상치 못한 예외는 500과 INTERNAL_SERVER_ERROR 코드를 반환하고 내부 메시지를 노출하지 않는다")
    void createTelemetry_unexpectedError_returnsInternalServerError() throws Exception {
        given(telemetryService.saveTelemetry(any()))
                .willThrow(new RuntimeException("boom - internal detail"));

        String body = """
                { "sensorId": 1, "value": 1.0, "latitude": 37.5, "longitude": 127.0 }
                """;

        mockMvc.perform(post("/api/telemetries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."));
    }

    @Test
    @DisplayName("bulk: 유효한 목록이면 202를 반환하고 배치 저장에 위임한다")
    void createTelemetriesInBulk_valid_returnsAccepted() throws Exception {
        String body = """
                [
                  { "sensorId": 1, "value": 10.0, "latitude": 37.5, "longitude": 127.0 },
                  { "sensorId": 2, "value": 20.0, "latitude": 37.6, "longitude": 127.1 }
                ]
                """;

        mockMvc.perform(post("/api/telemetries/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted());

        verify(telemetryService).saveTelemetriesInBulk(anyList());
    }

    @Test
    @DisplayName("이력 조회: content와 페이지 메타를 담은 PageResponse를 반환한다")
    void getTelemetryBySensor_returnsPageResponse() throws Exception {
        Telemetry telemetry = Telemetry.builder()
                .id(1L)
                .sensorId(9L)
                .value(42.0)
                .timestamp(LocalDateTime.now())
                .build();
        Page<Telemetry> page = new PageImpl<>(List.of(telemetry), PageRequest.of(0, 20), 1);
        given(telemetryService.getTelemetryHistory(eq(9L), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/api/telemetries/sensor/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].value").value(42.0))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    @DisplayName("stats: 기간 집계를 200과 함께 반환한다")
    void getTelemetryStats_returnsStats() throws Exception {
        TelemetryStatsResponse stats = new TelemetryStatsResponse(
                9L,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 2, 0, 0),
                3L, 20.0, 10.0, 30.0);
        given(telemetryService.getTelemetryStats(eq(9L), any(), any())).willReturn(stats);

        mockMvc.perform(get("/api/telemetries/sensor/9/stats")
                        .param("from", "2026-01-01T00:00:00")
                        .param("to", "2026-01-02T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.average").value(20.0))
                .andExpect(jsonPath("$.minimum").value(10.0))
                .andExpect(jsonPath("$.maximum").value(30.0));
    }

    @Test
    @DisplayName("위도가 허용 범위를 벗어나면 400과 INVALID_REQUEST 코드를 반환한다")
    void nearbyTelemetries_invalidLatitude_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/telemetries/nearby")
                        .param("lat", "999")
                        .param("lon", "127.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("정상 요청이면 200과 저장된 텔레메트리를 반환한다")
    void createTelemetry_success_returnsOk() throws Exception {
        Telemetry saved = Telemetry.builder()
                .id(1L)
                .sensorId(1L)
                .value(21.5)
                .timestamp(LocalDateTime.now())
                .build();
        given(telemetryService.saveTelemetry(any())).willReturn(saved);

        String body = """
                { "sensorId": 1, "value": 21.5, "latitude": 37.5, "longitude": 127.0 }
                """;

        mockMvc.perform(post("/api/telemetries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.value").value(21.5));
    }
}
