package com.sol.ecopulse.domain.telemetry;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@Entity
@Table(name = "telemetries", indexes = {
        @Index(name = "idx_sensor_timestamp", columnList = "sensor_id, timestamp") // 복합 인덱스 설계
})
@Getter
@NoArgsConstructor
public class Telemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_id", nullable = false)
    private Long sensorId;

    @Column(nullable = false)
    private Double value;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}