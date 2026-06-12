import asyncio
from datetime import datetime, timedelta  # 🎯 timedelta를 여기서 가져옵니다!
from dataclasses import dataclass
from temporalio import activity, workflow

# 1. 자바 백엔드로 보낼 요청 데이터 구조 정의
@dataclass
class TelemetryPayload:
    sensorId: int
    value: float
    timestamp: str

# 2. 데이터 생성과 전송을 모두 담당하는 Activity
@activity.defn
async def send_telemetry_to_springboot(sensor_id: int) -> str:
    import requests
    import random

    url = "http://localhost:8080/api/telemetries"

    fake_value = round(random.uniform(20.0, 30.0), 1)
    current_time = datetime.now().isoformat()

    data = {
        "sensorId": sensor_id,
        "value": fake_value,
        "timestamp": current_time
    }

    try:
        response = requests.post(url, json=data, timeout=5)
        if response.status_code == 200:
            return f"Success: Sent value {fake_value} to Sensor {sensor_id}"
        else:
            raise Exception(f"Spring Boot returned status code {response.status_code}")
    except requests.exceptions.RequestException as e:
        print(f"[Activity Error] 스프링 부트 연결 실패. 재시도 대기 중... Reason: {e}")
        raise e

# 3. 오직 흐름(시간차 제어)만 관리하는 결정적(Deterministic) Workflow
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

            # 3초 대기 후 다음 루프 실행
            await workflow.sleep(timedelta(seconds=3))  # 🎯 여기도 바로 timedelta 적용!

        return results