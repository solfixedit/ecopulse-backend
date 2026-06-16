import asyncio
import os
from datetime import datetime, timedelta  # 🎯 timedelta를 여기서 가져옵니다!
from dataclasses import dataclass
from temporalio import activity, workflow

# 1. 자바 백엔드로 보낼 요청 데이터 구조 정의
@dataclass
class TelemetryPayload:
    sensorId: int
    value: float
    timestamp: str

# 2. 데이터 생성과 전송을 모두 담당하는 Activity (수정본)
@activity.defn
async def send_telemetry_to_springboot(sensor_id: int) -> str:
    import requests
    import random

    base_url = os.getenv("SPRING_BOOT_BASE_URL", "http://localhost:8080")
    url = f"{base_url}/api/telemetries"

    # 기본 환경 데이터 생성
    fake_value = round(random.uniform(20.0, 30.0), 1)
    current_time = datetime.now().isoformat()

    # 🎯 [추가] 노스요크(North York) 부근의 가상 GPS 좌표 생성
    # 위도(Latitude): 약 43.75 ~ 43.77, 경도(Longitude): 약 -79.42 ~ -79.40
    fake_lat = round(random.uniform(43.750, 43.770), 6)
    fake_lon = round(random.uniform(-79.420, -79.400), 6)

    # 🎯 자바 백엔드 DTO 매핑명(latitude, longitude)과 정확히 일치시켜 딕셔너리에 추가합니다.
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
            # 로그에 좌표도 같이 찍히도록 가볍게 수정
            return f"Success: Sent value {fake_value} at ({fake_lat}, {fake_lon}) to Sensor {sensor_id}"
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

