import sbt.*

object Dependencies {
  object Version {
    // val catsEffect = "3.5.4"
    val fs2 = "3.11.0"

    val fs2mqtt = "1.0.1"
    val fs2kafka = "3.5.1"

    val http4s = "0.23.27"
    val http4sBlaze = "0.23.16"

    val jsoniter = "2.30.9"
    val chameleon = "0.4.1"

    val slf4j2 = "2.23.1"
    val log4cats = "2.7.0"

    val pureConfig = "0.17.7"

    val mockito = "3.2.10.0"
    val scalaTest = "3.2.10"
  }

  object CompileScope {
    val deps: List[ModuleID] = List(
      "co.fs2" %% "fs2-core" % Version.fs2,
      "co.fs2" %% "fs2-io" % Version.fs2,
      "co.fs2" %% "fs2-reactive-streams" % Version.fs2,

      "net.sigusr" %% "fs2-mqtt" % Version.fs2mqtt,
      "com.github.fd4s" %% "fs2-kafka" % Version.fs2kafka,

      "org.http4s" %% "http4s-dsl" % Version.http4s,
      "org.http4s" %% "http4s-blaze-server" % Version.http4sBlaze,

      "org.typelevel" %% "log4cats-slf4j" % Version.log4cats,
      "org.apache.logging.log4j" % "log4j-slf4j2-impl" % Version.slf4j2,

      "com.github.pureconfig" %% "pureconfig-core" % Version.pureConfig,
      "com.github.pureconfig" %% "pureconfig-cats-effect" % Version.pureConfig,
      "com.github.pureconfig" %% "pureconfig-enumeratum" % "0.17.7",

      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % Version.jsoniter,
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % Version.jsoniter,

      "com.github.cornerman" %% "chameleon" % Version.chameleon,
      "com.github.cornerman" %% "chameleon-http4s" % Version.chameleon
    )
  }

  object TestScope {
    val weaverCats = "com.disneystreaming" %% "weaver-cats" % "0.8.3" % Test

    val deps: List[ModuleID] = List(
      weaverCats
    )
  }

  object IntegrationTestScope {
    val weaverCats = "com.disneystreaming" %% "weaver-cats" % "0.8.3" % Test
    val testContainers = "com.dimafeng" %% "testcontainers-scala-core" % "0.41.0" % Test
    val testContainersKafka = "com.dimafeng" %% "testcontainers-scala-kafka" % "0.41.0" % Test
    val pahoMqtt = "org.eclipse.paho" % "org.eclipse.paho.client.mqttv3" % "1.2.5" % Test

    val deps: List[ModuleID] = List(
      weaverCats, testContainers, testContainersKafka, pahoMqtt
    )
  }
}
