# kafka-to-fabric-streamer

Bridge drilling telemetry from on-premises Kafka into Microsoft Fabric through Azure Event Hubs. This mirrors an oil-and-gas telemetry workflow: rigs stream rotary speed (RPM), weight on bit, and torque into local Kafka, and the app relays those events so Fabric can feed analytics, AI-driven drilling optimisation, and operational dashboards. The project boots a Quarkus application with two core components:

- **The Rig** produces synthetic drilling parameters every 100 ms and writes them into a Kafka topic.
- **The Bridge** consumes those messages and forwards them to Azure Event Hubs using SmallRye Reactive Messaging, applying backpressure when Event Hubs slows down.

## Prerequisites

- Java 17+
- Maven 3.9+
- Docker (for local Kafka via Redpanda)
- Azure Event Hubs namespace and connection string for cloud forwarding

## Local development

1. **Start Redpanda and the app**

   ```bash
   docker compose up --build
   ```

   The Quarkus app runs on http://localhost:8080. Telemetry is published to the `drilling-parameters` topic.

2. **Monitor Kafka**

   Use `rpk topic consume drilling-parameters -b localhost:29092` in another shell to inspect the generated payloads.

3. **Stop the stack**

   ```bash
   docker compose down -v
   ```

## Connecting to Azure Event Hubs

1. Export the required configuration values before launching the app (or add them to `docker-compose.yml`).

   ```bash
   export EVENTHUBS_ENABLED=true
   export EVENTHUBS_CONNECTION_STRING="Endpoint=sb://<namespace>.servicebus.windows.net/;SharedAccessKeyName=<keyname>;SharedAccessKey=<key>;EntityPath=<event-hub-name>"
   export EVENTHUBS_HUB_NAME=<event-hub-name>
   ```

2. Start the application locally with Maven if you do not wish to use Docker.

   ```bash
   mvn quarkus:dev
   ```

3. Once the variables are set, the bridge will publish incoming messages into Event Hubs. Failures automatically trigger `nack` to apply backpressure until the send completes successfully.

### Where to specify Azure configuration

- **Docker Compose:** Update the `EVENTHUBS_*` entries under the `streamer` service in docker-compose.yml or provide a `.env` file that docker compose will load automatically.
- **Local runs:** Export the same `EVENTHUBS_*` environment variables in your shell before running `mvn quarkus:dev`, or add them to application.properties for a permanent local default.

## Configuration reference

- `KAFKA_BOOTSTRAP_SERVERS` – Kafka bootstrap servers (default `localhost:29092`).
- `EVENTHUBS_ENABLED` – Enable forwarding to Event Hubs (default `false`).
- `EVENTHUBS_CONNECTION_STRING` – Event Hub connection string (required when enabled).
- `EVENTHUBS_HUB_NAME` – Target Event Hub name (default `drilling-telemetry`).

## Build

```bash
mvn clean package
```

The resulting JVM runner `quarkus-run.jar` is packaged under `target/quarkus-app/` and is used by the Docker image.

## Tests

```bash
mvn test
```

Integration tests run with the in-memory SmallRye connector so they do not require external Kafka or Azure services.
