package com.sol.ecopulse.domain.sensor;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point; // PostGIS 공간 정보 라이브러리

@Entity
@Table(name = "sensors")
@Getter
@Setter
@NoArgsConstructor
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type; // Example: WATER_LEVEL, TEMPERATURE

    // PostGIS point used for coordinates and spatial indexing.
    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;
}