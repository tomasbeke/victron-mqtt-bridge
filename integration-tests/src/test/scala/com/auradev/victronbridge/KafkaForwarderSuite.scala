package com.auradev.victronbridge

import cats.effect.{IO, Resource, Deferred}
import cats.syntax.all.*
import com.dimafeng.testcontainers.KafkaContainer
import fs2.Stream
import fs2.kafka.*
import org.testcontainers.utility.DockerImageName
import weaver.IOSuite

import scala.concurrent.duration.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import com.auradev.victronbridge.model.VictronValueEvent
import com.auradev.victronbridge.processor.EventForwarders
import com.auradev.victronbridge.util.ContainerResource

object KafkaForwarderSuite extends IOSuite:

  type Res = KafkaContainer

  private lazy val kafkaContainer = KafkaContainer(
    dockerImageName = DockerImageName.parse("confluentinc/cp-kafka:7.2.0")
  )

  def sharedResource: Resource[IO, Res] = ContainerResource(IO.pure(kafkaContainer))

  test("Events should be sent to Kafka topic with proper key"): kafkaContainer =>
    implicit val victronValueEventCodec: JsonValueCodec[VictronValueEvent] =
      JsonCodecMaker.make[VictronValueEvent](CodecMakerConfig)

    val topicName = "test-topic"
    val event = VictronValueEvent(key = "test-key", topic = "test-mqtt-topic", value = 1.0)
    val expectedJson = writeToString(event)
    val bootstrapServers = kafkaContainer.bootstrapServers

    val producerSettings = ProducerSettings[IO, String, String]
      .withBootstrapServers(bootstrapServers)

    val consumerSettings = ConsumerSettings[IO, String, String]
      .withBootstrapServers(bootstrapServers)
      .withGroupId("test-group")
      .withAutoOffsetReset(AutoOffsetReset.Earliest)

    for
      messageReceived <- Deferred[IO, (String, String)]

      producerStream = Stream.emit(event)
        .through(EventForwarders.kafkaProducerPipe(topicName, producerSettings))

      consumerStream = KafkaConsumer.stream(consumerSettings)
        .subscribeTo(topicName)
        .records
        .evalMap { committable =>
          val value = committable.record.value
          val key = committable.record.key
          messageReceived.complete((key, value))
        }
        .take(1)

      consumerFiber <- consumerStream.compile.drain.start
      _ <- IO.sleep(1.second)
      _ <- producerStream.compile.drain
      receivedMessage <- messageReceived.get.timeout(5.seconds)
      _ <- consumerFiber.cancel
    yield matches(receivedMessage):
      case (key, value) => expect.eql(key, event.key) && expect.eql(value, expectedJson)

