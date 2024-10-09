package com.auradev.victronbridge.processor

import cats.effect.{ Async, Sync }
import cats.syntax.all.{ catsSyntaxFlatMapOps, toFlatMapOps, toFunctorOps }
import com.auradev.victronbridge.model.VictronValueEvent
import com.github.plokhotnyuk.jsoniter_scala.core.*
import fs2.kafka.{ KafkaProducer, ProducerRecord, ProducerSettings }
import fs2.{ Pipe, Stream }
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object EventForwarders {
  private def logger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLoggerFromClass[F](this.getClass)

  def kafkaProducerPipe[F[_]: Async](
      topicName: String,
      producerSettings: ProducerSettings[F, String, String]
  ): Pipe[F, VictronValueEvent, Unit] = eventStream =>
    KafkaProducer
      .stream(producerSettings)
      .flatMap(producer =>
        eventStream.evalMap { event =>
          val jsonString = writeToString(event)
          logger.debug(s"Forwarding event: $jsonString") >>
            producer
              .produceOne(ProducerRecord(topicName, event.key, jsonString))
              .flatten
        }
      )
      .void
}
