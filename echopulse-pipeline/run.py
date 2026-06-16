import asyncio
import os
from temporalio.client import Client
from temporalio.worker import Worker
from pipeline import EcopulseDataWorkflow, send_telemetry_to_springboot

async def main():
    # 1. 로컬에 띄운 Temporal 서버에 연결
    temporal_address = os.getenv("TEMPORAL_ADDRESS", "localhost:7233")
    client = await Client.connect(temporal_address)

    # 2. 워크플로우와 액티비티를 등록한 일꾼(Worker) 생성
    worker = Worker(
        client,
        task_queue="ecopulse-task-queue",
        workflows=[EcopulseDataWorkflow],
        activities=[send_telemetry_to_springboot],
    )

    # 3. 백그라운드에서 Worker 시작
    asyncio.create_task(worker.run())
    print("🤖 Temporal Worker가 켜졌습니다. ecopulse-task-queue 리스닝 중...")

    # 4. 강남역 센서(ID 1번)에 대해 워크플로우 트리거 실행
    print("🚀 Ecopulse 환경 데이터 수집 워크플로우를 트리거합니다...")
    result = await client.execute_workflow(
        EcopulseDataWorkflow.run,
        args=[1],
        id="ecopulse-ingestion-job-001",
        task_queue="ecopulse-task-queue",
    )

    print(f"✅ 워크플로우 최종 결과 리포트: {result}")

if __name__ == "__main__":
    asyncio.run(main())