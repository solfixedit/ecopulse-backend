import asyncio
import os
from datetime import datetime, timedelta  # 🎯 timedelta를 여기서 가져옵니다!
from dataclasses import dataclass
from temporalio import activity, workflow

# Payload sent to the Java backend.
@dataclass
class TelemetryPayload:
    sensorId: int
    value: float
    timestamp: str

# Activity that generates sample telemetry and sends it to Spring Boot.
@activity.defn
async def send_telemetry_to_springboot(sensor_id: int) -> str:
    import requests
    import random

    base_url = os.getenv("SPRING_BOOT_BASE_URL", "http://localhost:8080")
    url = f"{base_url}/api/telemetries"

    # Generate a sample environmental reading.
    fake_value = round(random.uniform(20.0, 30.0), 1)
    current_time = datetime.now().isoformat()

    # Generate sample GPS coordinates near North York.
    # 위도(Latitude): 약 43.75 ~ 43.77, 경도(Longitude): 약 -79.42 ~ -79.40
    fake_lat = round(random.uniform(43.750, 43.770), 6)
    fake_lon = round(random.uniform(-79.420, -79.400), 6)

    # Match the field names expected by the Java request DTO.
    data = {
               "sensorId": sensor_id,
               "value": fake_value,
               "timestamp": current_time,
               "latitude": fake_lat,
               "longitude": fake_lon
    }

    try:
        response = requests.post(url, json=data, timeout=5)
        if response.status_code == 200:
            # Include coordinates in the activity result for easier debugging.
            return f"Success: Sent value {fake_value} at ({fake_lat}, {fake_lon}) to Sensor {sensor_id}"
        else:
            raise Exception(f"Spring Boot returned status code {response.status_code}")
    except requests.exceptions.RequestException as e:
        print(f"[Activity Error] 스프링 부트 연결 실패. 재시도 대기 중... Reason: {e}")
        raise e

# Workflow that controls the timing of telemetry delivery.
@workflow.defn
class EcopulseDataWorkflow:
    @workflow.run
    async def run(self, sensor_id: int) -> list:
        results = []

        for i in range(3):
            result = await workflow.execute_activity(
                send_telemetry_to_springboot,
                sensor_id,
                start_to_close_timeout=timedelta(seconds=10)  # 🎯 여기에 바로 timedelta 적용!
            )
            results.append(result)

            # Wait before sending the next sample.
            await workflow.sleep(timedelta(seconds=3))  # 🎯 여기도 바로 timedelta 적용!

        return results

