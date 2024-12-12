package com.auradev.victronbridge.processor

import cats.effect.{ Async, Concurrent, IO, Sync }
import com.auradev.victronbridge.model.VictronValueEvent
import fs2.concurrent.SignallingRef
import fs2.Pipe
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.Console as cc

object EventConsumers:
  private def logger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLoggerFromClass[F](this.getClass)

  def loggingPipe[F[_]: Async](): Pipe[F, VictronValueEvent, Unit] = _.evalMap: event =>
    logger.debug(
      s"Topic ${cc.CYAN}${event.key}${cc.RESET}: " +
        s"${cc.BOLD}${event.value}${cc.RESET}"
    )

  def cachingPipe[F[_]: Concurrent](
      cache: SignallingRef[F, Map[String, VictronValueEvent]]
  ): Pipe[F, VictronValueEvent, Unit] = _.evalMap: event =>
    cache.update(currentMap => currentMap + (event.key -> event))
