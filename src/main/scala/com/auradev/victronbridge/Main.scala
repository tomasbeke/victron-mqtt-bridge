package com.auradev.victronbridge

import cats.effect.std.Console
import cats.effect.{ ExitCode, IO, IOApp }
import com.auradev.victronbridge.config.ConfigLoader
import com.auradev.victronbridge.decoder.JSONMessageDecoder
import com.auradev.victronbridge.model.VictronValueEvent
import com.auradev.victronbridge.mqtt.MqttSubscriber
import com.auradev.victronbridge.processor.{ EventConsumers, EventForwarders }
import com.auradev.victronbridge.server.routes.RestRoutes
import fs2.concurrent.SignallingRef
import fs2.kafka.ProducerSettings
import fs2.{ Pipe, Stream }
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp:

  override def run(args: List[String]): IO[ExitCode] =
    implicit val console: Console[IO] = Console.make[IO]
    val logger: Logger[IO] = Logger[IO](Slf4jLogger.getLogger[IO])

    for
      config <- ConfigLoader.loadConfig()

      cache <- SignallingRef[IO, Map[String, VictronValueEvent]](Map.empty)
      kafkaProducerSettings =
        ProducerSettings[IO, String, String].withBootstrapServers(config.kafkaConfig.bootstrapServers)

      mqttSubscriber = new MqttSubscriber[IO, VictronValueEvent](
        host = config.mqttConfig.host,
        port = config.mqttConfig.port,
        user = config.mqttConfig.user,
        password = config.mqttConfig.password,
        transportType = config.mqttConfig.transportType,
        clientId = config.mqttConfig.clientId,
        topics = config.topics.map(_.topic),
        keepaliveTopic = config.keepaliveTopic,
        messageDecoder = JSONMessageDecoder.decode(MaskedMap(config.topics.map(t => t.topic -> t.alias).toMap))
      )

      mqttSubscriberStream = mqttSubscriber.mqttStream.broadcastThrough(
        EventConsumers.loggingPipe(),
        EventConsumers.cachingPipe(cache),
        EventForwarders.kafkaProducerPipe(config.kafkaConfig.topicName, kafkaProducerSettings)
      )

      httpApp = Router("/" -> RestRoutes[IO].routes(cache)).orNotFound
      server = BlazeServerBuilder[IO]
        .bindHttp(config.httpServerConfig.port, config.httpServerConfig.hostname)
        .withHttpApp(httpApp)
        .resource
        .useForever
        .void

      exitCode <- Stream(
        mqttSubscriberStream,
        Stream.eval(server)
      ).parJoinUnbounded.compile.drain.attempt
        .flatMap:
          case Left(e)  => logger.error(e)("Streams failed") >> IO.pure(ExitCode.Error)
          case Right(_) => IO.pure(ExitCode.Success)
    yield exitCode

end Main
