package com.auradev.victronbridge.config

import pureconfig._
import pureconfig.generic.derivation.default._
import pureconfig.module.enumeratum._
import enumeratum._
import enumeratum.EnumEntry._

case class AppConfig(
    mqttConfig: MqttConfig,
    topics: List[TopicConfig],
    keepaliveTopic: Option[String],
    kafkaConfig: KafkaConfig,
    httpServerConfig: HttpServerConfig,
    installationId: String
) derives ConfigReader

sealed trait TransportType extends EnumEntry with Snakecase

object TransportType extends Enum[TransportType]:
  val values = findValues

  case object Plain extends TransportType
  case object TLS extends TransportType
  case object TLSInsecure extends TransportType

case class TopicConfig(
    topic: String,
    alias: String
) derives ConfigReader

case class MqttConfig(
    host: String,
    port: Int,
    user: Option[String],
    password: Option[String],
    transportType: TransportType,
    clientId: String
) derives ConfigReader

case class KafkaConfig(
    bootstrapServers: String,
    topicName: String
) derives ConfigReader

case class HttpServerConfig(
    hostname: String,
    port: Int
) derives ConfigReader
