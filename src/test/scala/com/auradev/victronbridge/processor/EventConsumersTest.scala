package com.auradev.victronbridge.processor

import cats.effect.IO
import com.auradev.victronbridge.model.VictronValueEvent
import com.auradev.victronbridge.processor.EventConsumers.*
import fs2.concurrent.SignallingRef

object EventConsumersTest extends weaver.SimpleIOSuite:
  test("cachingPipe should cache the event"):
    for
      cache <- SignallingRef[IO, Map[String, VictronValueEvent]](Map.empty)
      event = VictronValueEvent("key", "topic", 1.0)
      _ <- fs2.Stream.emit(event).through(cachingPipe(cache)).compile.drain
      cached <- cache.get
    yield expect(cached.get("key").contains(event))
