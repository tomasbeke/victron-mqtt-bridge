package com.auradev.victronbridge.server.routes

import cats.effect.Sync
import cats.syntax.all.*
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import com.auradev.victronbridge.model.VictronValueEvent
import fs2.concurrent.SignallingRef
import chameleon.ext.http4s.JsonStringCodec.*
import chameleon.ext.jsoniter.*

class RestRoutes[F[_]: Sync] extends Http4sDsl[F]:
  def routes(cache: SignallingRef[F, Map[String, VictronValueEvent]]): HttpRoutes[F] = HttpRoutes.of[F]:
    case GET -> Root / "topic" / topicKey =>
      cache.get.flatMap: cachedValues =>
        cachedValues.get(topicKey) match
          case Some(event) => Ok(json(event))
          case None        => NotFound(s"No value found for topic: $topicKey")

    case GET -> Root / "topics" =>
      cache.get.flatMap: cachedValues =>
        Ok(json(cachedValues.values.toSeq))

    case GET -> Root / "health" / "status" => Ok()
