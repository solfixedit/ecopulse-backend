package com.sol.ecopulse.config;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.n52.jackson.datatype.jts.GeometryDeserializer;
import org.n52.jackson.datatype.jts.GeometrySerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpatialConfig {

    @SuppressWarnings("unchecked") // 컴파일러의 캐스팅 경고를 무시합니다.
    @Bean
    public SimpleModule jtsModule() {
        SimpleModule module = new SimpleModule("JtsSpatialModule");

        // 싱글톤으로 사용할 시리얼라이저/디시리얼라이저 인스턴스 생성
        GeometrySerializer serializer = new GeometrySerializer();
        GeometryDeserializer deserializer = new GeometryDeserializer();

        // 1. 최상위 Geometry 클래스 등록 (제네릭 캐스팅 적용)
        module.addSerializer(Geometry.class, (JsonSerializer<Geometry>) (JsonSerializer<?>) serializer);
        module.addDeserializer(Geometry.class, (JsonDeserializer<Geometry>) (JsonDeserializer<?>) deserializer);

        // 2. 에러가 발생했던 Point 클래스 등록
        // (JsonSerializer<Point>)로 강제 변환하여 컴파일러의 타입 바운드 제약을 우회합니다.
        module.addSerializer(Point.class, (JsonSerializer<Point>) (JsonSerializer<?>) serializer);
        module.addDeserializer(Point.class, (JsonDeserializer<Point>) (JsonDeserializer<?>) deserializer);

        return module;
    }
}