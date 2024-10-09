package com.auradev.victronbridge.server.routes

import org.http4s.HttpRoutes
import org.http4s.dsl.io.*
import cats.effect.IO
import com.auradev.victronbridge.model.VictronValueEvent
import fs2.concurrent.SignallingRef
import chameleon.ext.http4s.JsonStringCodec.*
import chameleon.ext.jsoniter.*

object RestRoutes {

  def routes(cache: SignallingRef[IO, Map[String, VictronValueEvent]]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "topic" / topicKey =>
      cache.get.flatMap { cachedValues =>
        cachedValues.get(topicKey) match {
          case Some(event) => Ok(json(event))
          case None        => NotFound(s"No value found for topic: $topicKey")
        }
      }
    case GET -> Root / "topics" =>
      cache.get.flatMap { cachedValues =>
        Ok(json(cachedValues.values.toSeq))
      }

    case GET -> Root / "health" / "status" => Ok()
  }
}
