package com.sol.ecopulse.config;

import org.n52.jackson.datatype.jts.JtsModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpatialConfig {

    // API 응답 시 GeoJSON(JTS Geometry)을 올바르게 직렬화하기 위한 잭슨 모듈 설정
    @Bean
    public JtsModule jtsModule() {
        return new JtsModule();
    }
}