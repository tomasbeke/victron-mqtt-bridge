package com.auradev.victronbridge.decoder

import com.auradev.victronbridge.MaskedMap
import net.sigusr.mqtt.api.Message

object JSONMessageDecoderTest extends weaver.FunSuite:
  val testTopic = "test"
  val testAlias = "testAlias"
  private val keyAliases = MaskedMap(Map(testTopic -> testAlias))
  val validDoubleValueJSON = """{"value": 1.0}"""
  val invalidDoubleValueJSON = """{"value": "invalid"}"""

  test("decode valid JSON to Right"):
    val message = toMessage(validDoubleValueJSON, testTopic)
    val event = JSONMessageDecoder.decode(keyAliases)(message)

    expect(event.isRight)

    event match
      case Right(event) =>
        expect(event.key == testAlias)
        expect(event.topic == testTopic)
        expect(event.value == 1.0)
      case _ => expect(false)

  test("fail on invalid JSON with Left"):
    val message = toMessage(invalidDoubleValueJSON, testTopic)
    val event = JSONMessageDecoder.decode(keyAliases)(message)

    expect(event.isLeft)

  test("decode JSON with unmapped key"):
    val unknownTopic = "unknown"
    val message = toMessage(validDoubleValueJSON, unknownTopic)
    val event = JSONMessageDecoder.decode(keyAliases)(message)

    expect(event.isRight)

    event match
      case Right(event) =>
        expect(event.key == unknownTopic)
        expect(event.topic == unknownTopic)
        expect(event.value == 1.0)
      case _ => expect(false)

  private def toMessage(payload: String, topic: String) = Message(
    topic = topic,
    payload = payload.getBytes("UTF-8").toVector
  )
