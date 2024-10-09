lazy val `victron-mqtt-bridge` =
  project
    .in(file("."))
    .settings(settings)
    .enablePlugins(JavaAppPackaging, DockerPlugin, AshScriptPlugin)

lazy val settings = {
  commonSettings ++
    dependencies ++
    sbtSettings ++
    scalafmtSettings ++
    dockerSettings
}

import com.typesafe.sbt.SbtNativePackager.autoImport.NativePackagerHelper.*
lazy val commonSettings =
  Seq(
    name := "victron-mqtt-bridge",
    maintainer := "tom@auradevelopment.sk",
    startYear := Some(2024),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalaVersion := "3.4.2",
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-encoding",
      "UTF-8"
    ),
    Compile / packageDoc / publishArtifact := false,
    Compile / packageSrc / publishArtifact := false,
    (Universal / mappings) ++= {
      val dir = (Compile / sourceDirectory).value / "resources"
      (dir ** AllPassFilter) pair rebase(dir, "conf")
    },
    Universal / javaOptions ++= globalJavaOptions,
    bashScriptExtraDefines ++= IO.readLines(
      baseDirectory.value / "project" / "native_packager_parameter.sh"
    ),
    topLevelDirectory := None,
    resolvers += Resolver.defaultUserFileRepository("cache")
  )

lazy val registry = sys.env.get("DOCKER_REGISTRY_BASE").orElse(Some("localhost:5000"))
lazy val projectName = sys.env.get("PROJECT_NAME").orElse(Some("home-monitoring"))
lazy val imageVersion = sys.env.getOrElse("CONTAINER_IMAGE_VERSION", "latest")
lazy val repository = Some(Seq(registry, projectName).flatten.mkString("/")).filter(_.trim.nonEmpty)

lazy val dockerSettings = Seq(
  dockerBaseImage := "eclipse-temurin:21-jre-alpine",
  Docker / dockerExposedPorts := Seq(8080),
  Docker / packageName := "victron-mqtt-bridge",
  Docker / version := imageVersion,
  Docker / dockerRepository := repository
)

// Java Options set via sbt have to be added explicitly to native packager
// Maintain the list here and reference in relevant scopes.
lazy val globalJavaOptions = Seq(
  // This service is headless.
  "-Djava.awt.headless=true"
)

lazy val dependencies = Seq(libraryDependencies ++= Dependencies.CompileScope.deps ++ Dependencies.TestScope.deps)

lazy val integration = (project in file("integration-tests"))
  .dependsOn(`victron-mqtt-bridge` % "provided -> provided;compile -> compile;test -> test; runtime -> runtime")
  .settings(
    name := "integration-tests",
    scalaVersion := "3.4.2",
    fork := true,
    publish / skip := true,
    parallelExecution := false,
    libraryDependencies ++= Dependencies.IntegrationTestScope.deps,
    testFrameworks += new TestFramework("weaver.framework.CatsEffect")
  )

testFrameworks += new TestFramework("weaver.framework.CatsEffect")

lazy val sbtSettings =
  Seq(
    fork := true,
    Global / cancelable := true
  )

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true
  )