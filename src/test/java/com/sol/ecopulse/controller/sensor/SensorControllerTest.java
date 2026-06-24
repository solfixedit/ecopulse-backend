package com.sol.ecopulse.controller.sensor;

import com.sol.ecopulse.service.sensor.SensorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SensorController.class)
class SensorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SensorService sensorService;

    @Test
    @DisplayName("센서 이름이 비어 있으면 400과 INVALID_REQUEST 코드, 필드 에러 목록을 반환한다")
    void createSensor_blankName_returnsBadRequest() throws Exception {
        String body = """
                { "name": "", "type": "AIR", "latitude": 37.5, "longitude": 127.0 }
                """;

        mockMvc.perform(post("/api/sensors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("위도가 허용 범위를 벗어나면 400과 INVALID_REQUEST 코드를 반환한다")
    void nearbySensors_invalidLatitude_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/sensors/nearby")
                        .param("latitude", "999")
                        .param("longitude", "127.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
