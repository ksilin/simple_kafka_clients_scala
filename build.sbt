// *****************************************************************************
// Build settings
// *****************************************************************************

inThisBuild(
  Seq(
    organization     := "example.com",
    organizationName := "ksilin",
    maintainer       := "konstantin.silin@gmail.com",
    version          := "0.0.1",
    startYear        := Some(2022),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalaVersion := "2.13.9",
    // assembly / assemblyJarName := "utils.jar",
    // assembly / assemblyMergeStrategy := {
    //  case PathList("module-info.class") => MergeStrategy.deduplicate
    // case PathList("META-INF", "versions", xs @ _, "module-info.class") => MergeStrategy.last
    //  case _ => MergeStrategy.preferProject
    // },
    // assembly / mainClass := Some("com.example.apps.HeaderBytesPrinter"),
    scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-Ywarn-unused:imports",
      // "-Xfatal-warnings",
    ),
    scalafmtOnCompile := true,
    dynverSeparator   := "_", // the default `+` is not compatible with docker tags
    resolvers ++= Seq(
      "confluent" at "https://packages.confluent.io/maven",
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

lazy val simple_kafka_clients_scala =
  project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin, JavaAppPackaging)
    .settings(commonSettings)
    .settings(
      libraryDependencies ++= Seq(
        library.clients,
        library.kafka,
        library.betterFiles,
        library.config,
        library.gson,
        library.circe,
        library.circeGeneric,
        library.circeParser,
        library.circeKafka,
        library.airframeLog,
        library.picoCli,
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
      val kafka       = "3.4.0"
      val betterFiles = "3.9.2"
      val airframeLog = "21.12.1"
      val config      = "1.4.2"
      val gson        = "2.10.1"
      val circeKafka  = "3.3.1"
      val circe       = "0.14.4"
      val picoCli     = "4.7.1"
      val logback     = "1.3.5"
      val scalatest   = "3.2.15"
    }

    val clients      = "org.apache.kafka"      % "kafka-clients" % Version.kafka
    val kafka        = "org.apache.kafka"     %% "kafka"         % Version.kafka
    val betterFiles  = "com.github.pathikrit" %% "better-files"  % Version.betterFiles
    val airframeLog  = "org.wvlet.airframe"   %% "airframe-log"  % Version.airframeLog
    val config       = "com.typesafe"          % "config"        % Version.config
    val gson         = "com.google.code.gson"  % "gson"          % Version.gson
    val circeKafka   = "com.nequissimus"      %% "circe-kafka"   % Version.circeKafka
    val circe        = "io.circe"             %% "circe-core"    % Version.circe
    val circeGeneric = "io.circe"             %% "circe-generic" % Version.circe
    val circeParser  = "io.circe"             %% "circe-parser"  % Version.circe
    val picoCli      = "info.picocli"          % "picocli"       % Version.picoCli
    val logback      = "ch.qos.logback"        % "logback-core"  % Version.logback
    val scalatest    = "org.scalatest"        %% "scalatest"     % Version.scalatest
  }
