import asyncio
import os
from temporalio.client import Client
from temporalio.worker import Worker
from pipeline import EcopulseDataWorkflow, send_telemetry_to_springboot

async def main():
    # Connect to the local Temporal server.
    temporal_address = os.getenv("TEMPORAL_ADDRESS", "localhost:7233")
    client = await Client.connect(temporal_address)

    # Register the workflow and activity with the worker.
    worker = Worker(
        client,
        task_queue="ecopulse-task-queue",
        workflows=[EcopulseDataWorkflow],
        activities=[send_telemetry_to_springboot],
    )

    # Start the worker in the background.
    asyncio.create_task(worker.run())
    print("Temporal Worker started. Listening on ecopulse-task-queue...")

    # Trigger the telemetry ingestion workflow for sensor ID 1.
    print("Starting the Ecopulse telemetry ingestion workflow...")
    result = await client.execute_workflow(
        EcopulseDataWorkflow.run,
        args=[1],
        id="ecopulse-ingestion-job-001",
        task_queue="ecopulse-task-queue",
    )

    print(f"Workflow result: {result}")

if __name__ == "__main__":
    asyncio.run(main())