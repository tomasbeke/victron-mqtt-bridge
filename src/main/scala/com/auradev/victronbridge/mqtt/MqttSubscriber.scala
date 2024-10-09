package com.auradev.victronbridge.mqtt

import cats.effect.{ Async, Sync }
import cats.effect.syntax.all.*
import cats.effect.std.Console
import fs2.Stream
import net.sigusr.mqtt.api.*
import ConnectionFailureReason.*
import cats.syntax.all.*
import com.auradev.victronbridge.config.TransportType
import com.auradev.victronbridge.model.Event
import com.comcast.ip4s.{ Host, Port }
import net.sigusr.mqtt.api.QualityOfService.AtLeastOnce
import net.sigusr.mqtt.api.RetryConfig
import net.sigusr.mqtt.api.ConnectionState
import net.sigusr.mqtt.api.ConnectionState.{ Connected, Connecting, Disconnected, Error, SessionStarted }
import net.sigusr.mqtt.api.Errors.{ ConnectionFailure, ProtocolError }
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import retry.RetryPolicies

import scala.concurrent.duration.*

class MqttSubscriber[F[_]: Async, Out <: Event](
    host: String,
    port: Int,
    transportType: TransportType,
    user: Option[String],
    password: Option[String],
    clientId: String,
    topics: Seq[String],
    keepaliveTopic: Option[String],
    messageDecoder: Message => Either[String, Out],
    retryTimes: Int = 4
) extends MqttStreamOperations[F] {

  override val logger: Logger[F] = Logger[F](Slf4jLogger.getLogger[F])

  private val retryConfig = RetryConfig.Custom[F](
    RetryPolicies
      .limitRetries[F](retryTimes)
      .join(RetryPolicies.fullJitter[F](FiniteDuration(2, SECONDS)))
  )

  private val transportConfig = TransportConfig(
    host = Host.fromString(host).get,
    port = Port.fromInt(port).get,
    tlsConfig = transportType match {
      case TransportType.TLS         => Some(TLSConfig[F](TLSContextKind.System))
      case TransportType.TLSInsecure => Some(TLSConfig[F](TLSContextKind.Insecure))
      case _                         => None
    },
    retryConfig = retryConfig,
    traceMessages = false
  )

  private val sessionConfig = SessionConfig(
    clientId = clientId,
    cleanSession = true,
    user = user,
    password = password
  )

  private lazy val topicsWithQoS = withQoS(topics)

  def mqttStream(using Console[F]): Stream[F, Out] =
    Stream.resource(Session[F](transportConfig, sessionConfig)).flatMap { implicit session =>
      val sessionStatus: Stream[F, Unit] = session.state.discrete
        .evalMap(logSessionStatus)
        .evalMap(onSessionError)

      val keepalivePublisher: Stream[F, Unit] = keepaliveTopic.fold(Stream.empty.covary[F])(keepalivePublisherStream)

      val sideEffectStreams: Stream[F, Unit] = Stream(
        sessionStatus,
        keepalivePublisher
      ).parJoinUnbounded

      subscribeStream(topicsWithQoS)
        .map(messageDecoder)
        .evalTap {
          case Left(error) => logger.error(s"Failed to decode message: $error")
          case _           => Sync[F].unit
        }
        .collect { case Right(event) =>
          event
        }
        .concurrently(sideEffectStreams.drain)
        .onFinalize {
          unsubscribeStream(topicsWithQoS).compile.drain
            .timeout(3.seconds)
            .handleErrorWith { e =>
              logger.error(e)("Unsubscribe operation timed out or failed")
            }
        }
    }

  def withQoS(topics: Seq[String]): Vector[(String, QualityOfService)] =
    topics.map((_, AtLeastOnce)).toVector

  def logSessionStatus: ConnectionState => F[ConnectionState] =
    s =>
      (s match {
        case Error(ConnectionFailure(reason)) =>
          logger.error(s"${scala.Console.RED}Connection failure - ${reason.show}${scala.Console.RESET}")
        case Error(ProtocolError) =>
          logger.error(s"${scala.Console.RED}Ṕrotocol error${scala.Console.RESET}")
        case Disconnected =>
          logger.warn(s"${scala.Console.BLUE}Transport disconnected${scala.Console.RESET}")
        case Connecting(nextDelay, retriesSoFar) =>
          logger.info(
            s"${scala.Console.BLUE}Transport connecting. $retriesSoFar attempt(s) so far, next attempt in $nextDelay ${scala.Console.RESET}"
          )
        case Connected =>
          logger.info(s"${scala.Console.BLUE}Transport connected${scala.Console.RESET}")
        case SessionStarted =>
          logger.info(s"${scala.Console.BLUE}Session started${scala.Console.RESET}")
      }) >> Sync[F].pure(s)

  def onSessionError: ConnectionState => F[Unit] = {
    case Error(e) => Sync[F].raiseError(e)
    case _        => Sync[F].pure(())
  }
}

trait MqttStreamOperations[F[_]: Async] {
  def logger: Logger[F]

  def subscribeStream(topics: Vector[(String, QualityOfService)])(using session: Session[F]): Stream[F, Message] = for {
    s <- Stream.eval(session.subscribe(topics))
    _ <- Stream.eval(s.traverse { p =>
      logger.info(
        s"Topic ${scala.Console.CYAN}${p._1}${scala.Console.RESET} subscribed with QoS " +
          s"${scala.Console.CYAN}${p._2.show}${scala.Console.RESET}"
      )
    })
    messageStream <- session.messages
  } yield messageStream

  def keepalivePublisherStream(topic: String)(using session: Session[F]): Stream[F, Unit] = for {
    _ <- ticks(5.seconds)
    _ <- Stream.eval(
      logger.info(s"Publishing keepalive on topic ${scala.Console.CYAN}$topic${scala.Console.RESET}")
    )
    _ <- Stream.eval(session.publish(topic, payload(""), AtLeastOnce))
  } yield ()

  def unsubscribeStream(topics: Vector[(String, QualityOfService)])(using session: Session[F]): Stream[F, Unit] =
    Stream.eval(session.unsubscribe(topics.map(_._1))) >> Stream.eval(
      logger.info(s"${scala.Console.RED}Unsubscribed from topics${scala.Console.RESET}")
    )

  def ticks(seconds: FiniteDuration): Stream[F, Unit] = Stream.awakeEvery[F](seconds).map(_ => ())

  val payload: String => Vector[Byte] = (_: String).getBytes("UTF-8").toVector
}
