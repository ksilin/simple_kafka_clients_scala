// *****************************************************************************
// Build settings
// *****************************************************************************

inThisBuild(
  Seq(
    organization := "example.com",
    organizationName := "ksilin",
    startYear := Some(2022),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalaVersion := "2.13.8",
    scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-Ywarn-unused:imports",
      //"-Xfatal-warnings",
    ),
    scalafmtOnCompile := true,
    dynverSeparator := "_", // the default `+` is not compatible with docker tags
    resolvers ++= Seq("confluent" at "https://packages.confluent.io/maven",
      "ksqlDb" at "https://ksqldb-maven.s3.amazonaws.com/maven",
      Resolver.sonatypeRepo("releases"),
      Resolver.bintrayRepo("wolfendale", "maven"),
      Resolver.mavenLocal,
      "jitpack" at "https://jitpack.io"
    ),
    Test / fork := true, // required for setting env vars
  )
)

// *****************************************************************************
// Projects
// *****************************************************************************

lazy val producers =
  project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(commonSettings)
    .settings(
      libraryDependencies ++= Seq(
        library.clients,
        library.kafka,
        library.betterFiles,
        library.gson,
        library.airframeLog,
        library.logback,
        library.scalatest % Test
      ),
    )

// *****************************************************************************
// Project settings
// *****************************************************************************

lazy val commonSettings =
  Seq(
    // Also (automatically) format build definition together with sources
    Compile / scalafmt := {
      val _ = (Compile / scalafmtSbt).value
      (Compile / scalafmt).value
    },
  )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val kafka = "3.2.0"
      val betterFiles = "3.9.1"
      val airframeLog = "21.12.1"
      val gson = "2.9.0"
      val logback = "1.2.10"
      val scalatest = "3.2.10"
    }

    val clients = "org.apache.kafka" % "kafka-clients" % Version.kafka
    val kafka = "org.apache.kafka" %% "kafka" % Version.kafka
    val betterFiles = "com.github.pathikrit" %% "better-files" % Version.betterFiles
    val airframeLog = "org.wvlet.airframe" %% "airframe-log" % Version.airframeLog
    val gson  = "com.google.code.gson" % "gson" % Version.gson
    val logback = "ch.qos.logback" % "logback-core" % Version.logback
    val scalatest = "org.scalatest" %% "scalatest" % Version.scalatest
  }
