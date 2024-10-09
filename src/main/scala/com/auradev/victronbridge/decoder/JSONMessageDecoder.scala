package com.auradev.victronbridge.decoder

import com.auradev.victronbridge.MaskedMap
import com.auradev.victronbridge.model.{ MessageValue, VictronValueEvent }
import com.github.plokhotnyuk.jsoniter_scala.core.readFromString
import net.sigusr.mqtt.api.Message

import cats.syntax.all.catsSyntaxEither
import scala.util.Try

object JSONMessageDecoder {

  def decode(keyAliases: MaskedMap[String])(message: Message): Either[String, VictronValueEvent] = {
    val payload = new String(message.payload.toArray, "UTF-8")
    val messageValueEither = Try(readFromString[MessageValue](payload)).toEither
      .leftMap(_ => s"Could not deserialize JSON $payload")

    messageValueEither.map { messageValue =>
      VictronValueEvent(
        key = keyAliases.getMasked(message.topic).getOrElse(message.topic),
        topic = message.topic,
        value = messageValue.value
      )
    }
  }

}
