# EcoPulse: Real-Time IoT Telemetry & Geospatial Query API 🌍

A backend system for ingesting real-time environmental IoT sensor data and serving geospatial (GIS) proximity queries. The project demonstrates high-throughput bulk ingestion, PostGIS spatial indexing, and time-series read patterns on top of Spring Boot and PostgreSQL/PostGIS.

---

## 🚀 Key Engineering Areas

### 1. High-Throughput Data Ingestion (Bulk Insert)
- **Problem:** With `@GeneratedValue(strategy = GenerationType.IDENTITY)`, Hibernate cannot batch JDBC inserts, so streaming thousands of telemetry rows through `save()`/`saveAll()` incurs one round-trip per row.
- **Approach:** A dedicated `TelemetryBulkRepository` uses Spring `JdbcTemplate.batchUpdate()` to insert in chunks of 1,000, bypassing the per-row round-trips. The PostGIS `location` column is populated in-SQL via `ST_SetSRID(ST_MakePoint(lon, lat), 4326)`, so bulk-inserted rows are immediately searchable by the spatial queries.
- Exposed at `POST /api/telemetries/bulk`.

> **Note:** No throughput benchmark is committed to this repo yet — the bulk path is a structural optimization, not a measured figure. A JMH/gatling benchmark would be a good future addition.

### 2. Geospatial Proximity Queries (PostGIS + GiST)
- **Problem:** Radius search (e.g. "sensors within 5 km") degrades to a full scan when the predicate computes a distance for every row.
- **Approach:** Queries use `ST_DWithin(location::geography, :center::geography, :meters)` so the distance is geodesic (meters) **and** index-eligible. A **GiST index** on `(location::geography)` is created idempotently at startup by `SpatialIndexInitializer` — JPA's `@Index` cannot express an index method (GiST), so it is issued as raw DDL (`CREATE INDEX IF NOT EXISTS ... USING gist (...)`).
- Endpoints: `GET /api/sensors/nearby`, `GET /api/telemetries/nearby`.

### 3. Time-Series Reads (Composite Index + Pagination)
- **Problem:** Aggregating or listing a sensor's history over a large time range can scan unbounded rows and load them all into memory.
- **Approach:** A **composite index** on `(sensor_id, timestamp)` backs newest-first lookups, and history reads are **paginated** with Spring Data `Pageable`/`Page` (`?page=&size=`), returned in a stable `PageResponse` envelope. The same index also backs a windowed **aggregation** endpoint (`count`/`avg`/`min`/`max` over a `[from, to]` range).
- Endpoints: `GET /api/telemetries/sensor/{sensorId}?page=&size=`, `GET /api/telemetries/sensor/{sensorId}/stats?from=&to=`.

### 4. API Validation & Error Handling
- Request payloads and query parameters are validated with Jakarta Bean Validation (`@NotNull`, `@DecimalMin/Max`, `@Positive`, coordinate ranges).
- A `@RestControllerAdvice` (`GlobalExceptionHandler`) maps validation failures, not-found, and unexpected errors to a consistent `ErrorResponse` (code, message, field errors, timestamp), including a catch-all `500` that logs the stack trace without leaking internals.

---

## 🛠️ Tech Stack

- **Language & Framework:** Java 17 / Spring Boot 4.0.x
- **Database & GIS:** PostgreSQL / PostGIS (Hibernate Spatial + JTS)
- **Data Access:** Spring Data JPA + Spring `JdbcTemplate` (bulk insert)
- **Build:** Gradle
- **Containerization:** Docker / Docker Compose (local DB)
- **Testing:** JUnit 5, Mockito, MockMvc (`@WebMvcTest`), Testcontainers (PostGIS)

---

## 🔌 API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/sensors` | Register a sensor (`name`, `type`, `latitude`, `longitude`) |
| `GET`  | `/api/sensors/nearby?latitude=&longitude=&radiusKm=` | Sensors within a radius (km) |
| `POST` | `/api/telemetries` | Record a single telemetry reading |
| `POST` | `/api/telemetries/bulk` | Batch-ingest telemetry readings |
| `GET`  | `/api/telemetries/sensor/{sensorId}?page=&size=` | Paginated history (newest first) |
| `GET`  | `/api/telemetries/sensor/{sensorId}/stats?from=&to=` | Windowed aggregate (count / avg / min / max) |
| `GET`  | `/api/telemetries/nearby?lat=&lon=&radius=` | Telemetry within a radius (meters) |

---

## 📂 System Architecture Overview

```text
ecopulse-backend/
├── src/main/java/com/sol/ecopulse/
│   ├── config/                 # SpatialConfig (JTS Jackson), SpatialIndexInitializer (GiST), DataInitializer (seed)
│   ├── controller/             # sensor / telemetry REST controllers
│   ├── domain/
│   │   ├── sensor/             # Sensor entity (id, name, type, geometry location)
│   │   └── telemetry/          # Telemetry entity (sensorId, value, timestamp, geometry location)
│   ├── dto/                    # request/response records, ErrorResponse, PageResponse
│   ├── exception/              # GlobalExceptionHandler, NotFoundException
│   ├── repository/
│   │   ├── sensor/             # SensorRepository (Spring Data JPA)
│   │   └── telemetry/          # TelemetryRepository + JdbcTemplate bulk insert
│   └── service/                # sensor / telemetry business logic
└── src/main/resources/
    └── application.yml
```

---

## ✅ Testing

Run the suite with `./gradlew test`. Coverage spans three levels:

- **Unit (Mockito):** service logic — coordinate mapping, timestamp defaults, not-found propagation, bulk delegation.
- **Web slice (`@WebMvcTest`):** controller validation, error responses, pagination envelope.
- **Integration (`@SpringBootTest` + Testcontainers):** the PostGIS native queries (`ST_DWithin`), bulk insert persisting geometry, and DB-level pagination run against a real `postgis/postgis` container (started once per JVM).

---

## 🛠️ Data Ingestion Pipeline (Python & Temporal.io)

> **💡 Architecture Note**
> The Python data pipeline module in this project was highly inspired by the "Django + Temporal.io" architecture session at the **Toronto Python Meetup**. It was designed and introduced to experiment with a production-grade, fault-tolerant data pipeline that ensures resilient IoT telemetry ingestion, independent of core backend downtime.

### Architecture Overview
- **Core Backend**: Java, Spring Boot, PostGIS, PostgreSQL (Manages spatial-temporal data and provides core APIs)
- **Data Ingestion**: Python 3, Temporal.io (Orchestrates virtual IoT sensor data generation and guarantees reliable HTTP delivery via distributed workflow retries)

### Directory Structure
```text
ecopulse-backend/
├── src/main/java/...      # Core Spring Boot Application
└── ecopulse-pipeline/     # Python Data Pipeline Module (Temporal.io Worker)
    ├── pipeline.py        # Workflow & Activity definitions for data streaming
    └── run.py             # Worker initialization & Workflow execution trigger
```
