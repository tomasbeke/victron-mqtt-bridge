package com.auradev.victronbridge.model

import java.time.Instant
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

case class VictronValueEvent(key: String, topic: String, value: Double, timestamp: Instant = Instant.now())
    extends Event

object VictronValueEvent {
  given codec: JsonValueCodec[VictronValueEvent] = JsonCodecMaker.make
  given seqCodec: JsonValueCodec[Seq[VictronValueEvent]] = JsonCodecMaker.make
}
