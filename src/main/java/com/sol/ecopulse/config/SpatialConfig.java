package com.sol.ecopulse.config;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.locationtech.jts.geom.Geometry;
import org.n52.jackson.datatype.jts.GeometryDeserializer;
import org.n52.jackson.datatype.jts.GeometrySerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpatialConfig {

    @Bean
    public SimpleModule jtsModule() {
        SimpleModule module = new SimpleModule("JtsSpatialModule");

        // 중요: 제네릭 타입 캐스팅을 통해 가이드라인을 주어 컴파일 에러를 해결합니다.
        module.addSerializer(Geometry.class, (JsonSerializer<Geometry>) (JsonSerializer<?>) new GeometrySerializer());
        module.addDeserializer(Geometry.class, (JsonDeserializer<Geometry>) (JsonDeserializer<?>) new GeometryDeserializer());

        return module;
    }
}