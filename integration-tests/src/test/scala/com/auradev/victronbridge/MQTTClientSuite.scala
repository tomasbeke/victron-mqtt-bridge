package com.auradev.victronbridge

import cats.effect.{IO, Resource}
import com.auradev.victronbridge.config.TransportType
import com.auradev.victronbridge.model.Event
import com.auradev.victronbridge.mqtt.MqttSubscriber
import com.auradev.victronbridge.util.ContainerResource
import com.dimafeng.testcontainers.GenericContainer
import net.sigusr.mqtt.api.Message
import org.eclipse.paho.client.mqttv3.{MqttClient, MqttMessage}
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.MountableFile
import weaver.IOSuite

import java.time.Instant
import scala.concurrent.duration.DurationInt

object MQTTClientSuite extends IOSuite {

  type Res = GenericContainer

  val mosquittoConfigPath = "src/test/resources/mosquitto.conf"
  val mosquittoContainer = GenericContainer(
    dockerImage = "eclipse-mosquitto:latest",
    exposedPorts = Seq(1883),
    waitStrategy = Wait.forListeningPort()
  ).configure { container =>
    container.withCopyFileToContainer(
      MountableFile.forHostPath(mosquittoConfigPath),
      "/mosquitto/config/mosquitto.conf"
    )
  }

  def sharedResource: Resource[IO, Res] = ContainerResource(IO.pure(mosquittoContainer))

  case class MyEvent(key: String, topic: String, value: String, timestamp: Instant = Instant.now()) extends Event

  val messageDecoder: Message => Either[String, MyEvent] = msg =>
    Right(MyEvent(msg.topic, msg.topic, new String(msg.payload.toArray, "UTF-8")))

  test("MqttSubscriber should receive published messages") { mqttContainer =>
    for {
      host <- IO(mqttContainer.host)
      port <- IO(mqttContainer.mappedPort(1883))
      subscriber = new MqttSubscriber[IO, MyEvent] (
        host = host,
        port = port,
        user = None,
        password = None,
        transportType = TransportType.Plain,
        clientId = "test-subscriber",
        topics = Seq("test/topic"),
        keepaliveTopic = None,
        messageDecoder = messageDecoder,
        retryTimes = 1
      )
      receivedMessagesFiber <- subscriber.mqttStream.take(1).compile.toList.start

      _ <- IO.sleep(3.second)

      _ <- publishMessagesToBroker(host, port)
      messages <- receivedMessagesFiber.joinWithNever
      expectedMessages = List(MyEvent("test/topic", "test/topic", "test message"))
      _ <- IO(expect(messages == expectedMessages))
    } yield success
  }

  test("MqttSubscriber should send keep-alive messages") { mqttContainer =>
    for {
      host <- IO(mqttContainer.host)
      port <- IO(mqttContainer.mappedPort(1883))
      keepAliveSubscriber = new MqttSubscriber[IO, MyEvent](
        host = host,
        port = port,
        user = None,
        password = None,
        transportType = TransportType.Plain,
        clientId = "keep-alive-subscriber",
        topics = Seq("keep-alive/topic"),
        keepaliveTopic = None,
        messageDecoder = messageDecoder,
        retryTimes = 1
      )
      receivedMessagesFiber <- keepAliveSubscriber.mqttStream.take(1).compile.toList.start

      _ <- IO.sleep(1.second)

      mainSubscriber = new MqttSubscriber[IO, MyEvent](
        host = host,
        port = port,
        user = None,
        password = None,
        transportType = TransportType.Plain,
        clientId = "main-subscriber",
        topics = Seq("test/topic"),
        keepaliveTopic = Some("keep-alive/topic"),
        messageDecoder = messageDecoder,
        retryTimes = 1
      )
      mainSubscriberFiber <- mainSubscriber.mqttStream.compile.drain.start
      keepAliveMessages <- receivedMessagesFiber.joinWithNever
      _ <- mainSubscriberFiber.cancel
    } yield expect(keepAliveMessages.nonEmpty)
  }

  private def publishMessagesToBroker(host: String, port: Int): IO[Unit] = IO {
    val client = new MqttClient(s"tcp://$host:$port", MqttClient.generateClientId())
    client.connect()
    val message = new MqttMessage("test message".getBytes("UTF-8"))
    client.publish("test/topic", message)
    client.disconnect()
  }
}
