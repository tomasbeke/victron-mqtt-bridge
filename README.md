## MQTT - Kafka bridge for Victron Energy devices

This project is a simple bridge between MQTT and Kafka for Victron Energy devices (see [discussion](https://communityarchive.victronenergy.com/questions/155407/mqtt-local-via-mqtt-broker.html)) that subscribes to select topics and publishes the values to a Kafka topic.
It is written in Scala 3 and uses:
- fs2 - the [Functional Streams for Scala](https://fs2.io/) library,
- [fs2-mqtt](https://github.com/user-signal/fs2-mqtt) library to connect to MQTT,
- [fs2 Kafka](https://fd4s.github.io/fs2-kafka/) library to publish messages to Kafka,
- [Http4s](https://http4s.org/) library with Blaze server for a simple REST API,
- [Weaver](https://com.disneystreaming.weaver-test.com/docs/) for testing,
- [testcontainers-scala](https://github.com/testcontainers/testcontainers-scala) for integration tests,
- [jsoniter-scala](https://github.com/plokhotnyuk/jsoniter-scala) for JSON serialization/deserialization,
- [log4cats](https://github.com/typelevel/log4cats) for logging,
- [scalafmt](https://scalameta.org/scalafmt/) for code formatting,
- sbt 1.8.0 for building and publishing the project.

You can also use this project as a starting point for any other IoT device that publishes data to MQTT.

Disclaimer: While the project is functional, the primary motivation was to demonstrate the use of several functional Scala libraries in a real-world application. The configuration capabilities are limited and somewhat tailored to my specific use case.

### Additional features

#### Keepalive messages

It appears that the MQTT connection to the Victron MQTT broker is closed after a period of inactivity. To prevent this, the bridge sends a keepalive message to the broker every 5 seconds. The destination topic can be configured by the `keepalive-topic` config entry (can be omitted).

#### REST API
You can query the latest value of a specific key with a simple REST endpoint available at `http://localhost:8080/topic/{key}` where `{key}` is the key (alias).
An array of all values can be obtained at `http://localhost:8080/topics`.

There's a health check endpoint at `http://localhost:8080/health/status`.

### Configuration

The project uses [pureconfig](https://pureconfig.github.io/) for configuration. The configuration file is located in `src/main/resources/application.conf`. The configuration file is divided into several sections:

- `mqtt-config` - MQTT configuration, you can override the default values with environment variables:
  - MQTT_INSTALLATION_ID - the installation ID of the system
  - MQTT_HOST - the hostname of the MQTT broker
  - MQTT_PORT - the port of the MQTT broker
  - MQTT_TRANSPORT_TYPE - the transport type, `plain`, `tls` (relying on system for cert) or `tls_insecure` (ignores cert)
- `kafka-config` - Kafka configuration, you can override the default bootstrap server with KAFKA_BOOTSTRAP_SERVERS
- `http-server-config` - HTTP server configuration, you can override the default 8080 port with SERVICE_HTTP_PORT
- `topics` - an array of topics to subscribe to, each topic has an `alias` which will be used as a key in the Kafka message, and a `topic` which is the actual MQTT topic to subscribe to. Topic paths can use the `+` wildcard in segments.

### Integration tests

The project includes a simple integration test that uses a Docker container for mosquitto, testing the subscriber and keepalive.
As recommended in the release notes for [sbt 1.9.0](https://eed3si9n.com/sbt-1.9.0) a subproject is used instead of the `IntegrationTest` configuration. You can run them with:
```shell
sbt> project integration
sbt:integration> test
```

### Deployment

You can build and publish the project with `sbt publish`. DOCKER_REGISTRY_BASE is an environment variable that you can set to the base URL of your Docker registry.

A Helm chart and config files are located in the `deployment/helm` directory. You can deploy the bridge to Kubernetes with the `deployment/helm-deploy.sh` script as well. You'll likely need to tweak these to fit your environment, but it should give you a good starting point.


