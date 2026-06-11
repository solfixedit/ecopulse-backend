import asyncio
import random
from datetime import datetime
from dataclasses import dataclass
import requests
from temporalio import activity, workflow

# 1. 자바 백엔드로 보낼 요청 데이터 구조 정의
@dataclass
class TelemetryPayload:
    sensorId: int
    value: float
    timestamp: str

# 2. 실제 작업을 수행할 Activity 정의 (스프링 부트 호출)
@activity.defn
async def send_telemetry_to_springboot(payload: TelemetryPayload) -> str:
    url = "http://localhost:8080/api/telemetries"

    data = {
        "sensorId": payload.sensorId,
        "value": payload.value,
        "timestamp": payload.timestamp
    }

    try:
        response = requests.post(url, json=data, timeout=5)
        if response.status_code == 200:
            return f"Success: Sent value {payload.value} to Sensor {payload.sensorId}"
        else:
            raise Exception(f"Spring Boot returned status code {response.status_code}")
    except requests.exceptions.RequestException as e:
        print(f"[Activity Error] 스프링 부트 연결 실패. 재시도 대기 중... Reason: {e}")
        raise e

# 3. 전체 흐름을 제어할 Workflow 정의
@workflow.defn
class EcopulseDataWorkflow:
    @workflow.run
    async def run(self, sensor_id: int) -> list:
        results = []

        # 3번 연속으로 가상 데이터를 시간차를 두고 전송
        for i in range(3):
            fake_value = round(random.uniform(20.0, 30.0), 1)
            current_time = datetime.now().isoformat()

            payload = TelemetryPayload(
                sensorId=sensor_id,
                value=fake_value,
                timestamp=current_time
            )

            result = await workflow.execute_activity(
                send_telemetry_to_springboot,
                payload,
                start_to_close_timeout=asyncio.timedelta(seconds=10)
            )
            results.append(result)

            await workflow.sleep(asyncio.timedelta(seconds=3))

        return results