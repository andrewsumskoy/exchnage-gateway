
val akkaV = "2.5.16"
val xchangeV = "4.3.9"

lazy val commonSettings = Seq(
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.12.6",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-slf4j" % akkaV,
    "com.github.nscala-time" %% "nscala-time" % "2.20.0",
    "org.slf4j" % "slf4j-api" % "1.7.25",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "org.scalatest" %% "scalatest" % "3.0.5" % Test
  )
)

lazy val server = project.in(file("server"))
    .settings(commonSettings)
    .settings(
      name := "exchange-gateway-server",
      libraryDependencies ++= Seq(
        "javax.ws.rs" % "javax.ws.rs-api" % "2.1" artifacts Artifact("javax.ws.rs-api", "jar", "jar"),
        "org.knowm.xchange" % "xchange-core" % xchangeV,
        "org.knowm.xchange" % "xchange-livecoin" % xchangeV,
        "org.knowm.xchange" % "xchange-bitfinex" % xchangeV
      )
    )
