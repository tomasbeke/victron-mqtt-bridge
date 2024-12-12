package com.auradev.victronbridge.model

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

final case class MessageValue(value: Double)

object MessageValue:
  given codec: JsonValueCodec[MessageValue] = JsonCodecMaker.make
