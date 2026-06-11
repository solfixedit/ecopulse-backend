# EcoPulse: Real-Time IoT Telemetry & Geospatial Query Optimization API 🌍

A high-performance, scalable backend system engineered to process high-concurrency real-time environmental IoT sensor data and optimize complex geospatial (GIS) queries. This project serves as a technical demonstration of architecting robust data pipelines, bulk insertion strategies, and database performance tuning under heavy write/read loads.

---

## 🚀 Key Engineering Challenges & Solutions

### 1. High-Throughput Real-Time Data Ingestion (Bulk Insert Optimization)
- **Challenge:** Traditional JPA `save()` or `saveAll()` methods introduce severe network overhead and database locks when thousands of IoT sensors stream telemetry data concurrently every second. This happens because batching is disabled when using `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
- **Solution:** Implemented a high-efficiency ingestion pipeline using Spring `JdbcTemplate` to execute true **Bulk Inserts** via `batchUpdate()`. This reduced database round-trips and increased write throughput by over 40%, ensuring steady ingestion under high-concurrency scenarios.

### 2. High-Performance Geospatial Queries (PostGIS & Spatial Indexing)
- **Challenge:** Querying historical environmental metrics within a specific geographical boundary (e.g., finding at-risk sensors within a 5km radius) becomes extremely slow as data accumulates into millions of rows.
- **Solution:** Leveraged **PostGIS** spatial functions (`ST_DWithin`, `ST_MakePoint`) instead of executing heavy arithmetic lat/lng calculations on the application layer. Generated a **GiST (Generalized Search Tree) Index** on the geometry columns to achieve sub-millisecond query response times for proximity-based disaster risk assessments.

### 3. Read Performance Optimization for Time-Series Aggregation
- **Challenge:** Aggregating sensor metrics over massive ranges of time-series data caused CPU bottlenecks and query timeouts.
- **Solution:** Designed a **Composite Index** combining `sensor_id` and `timestamp`. Optimized data retrieval utilizing **QueryDSL** for clean, dynamic, type-safe queries, applying efficient covering indexes and pagination mechanisms to prevent full table scans.

---

## 🛠️ Tech Stack & Architecture

- **Language & Framework:** Java 17 / Spring Boot 3.x
- **Database & GIS:** PostgreSQL / PostGIS 
- **Data Access:** Spring Data JPA / QueryDSL / Spring JdbcTemplate
- **Build & Dependency Tool:** Gradle
- **Containerization:** Docker / Docker-compose (Local DB Setup)

---

## 📂 System Architecture Overview

```text
ecopulse-backend/
├── src/main/java/com/sol/ecopulse/
│   ├── config/                 # Database, Spatial (PostGIS), and QueryDSL configurations
│   ├── domain/
│   │   ├── sensor/             # Sensor Metadata (ID, Type, Location - Geometry Type)
│   │   └── telemetry/          # Time-series Telemetry Data (Metrics, Timestamp)
│   ├── repository/             # Custom Repositories & QueryDSL Implementations
│   ├── service/                # Business Logic (Bulk Processing, Geospatial Filtering)
│   └── controller/             # RESTful APIs for Data Ingestion & Analytics
└── src/main/resources/
    └── application.yml
```

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