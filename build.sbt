ThisBuild / scalaVersion := "2.13.12"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "ru.filippov.otus"
ThisBuild / organizationName := "roombooking"

enablePlugins(JavaAppPackaging)

lazy val root = (project in file("."))
  .settings(
    name := "room-booking-service",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-ember-client" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-literal" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.postgresql" % "postgresql" % postgresVersion,
      "io.getquill" %% "quill-jdbc" % quillVersion,
      "io.getquill" %% "quill-core" % quillVersion,
      "io.getquill" %% "quill-sql" % quillVersion,
      "org.liquibase" % "liquibase-core" % liquibaseVersion,
      "com.github.pureconfig" %% "pureconfig" % pureconfigVersion,
      "com.github.pureconfig" %% "pureconfig-cats-effect" % pureconfigVersion,
      "com.typesafe" % "config" % "1.4.3",
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test
    )
  )

val http4sVersion = "0.23.25"
val circeVersion = "0.14.6"
val postgresVersion = "42.7.2"
val quillVersion = "4.8.0"
val liquibaseVersion = "4.25.0"
val pureconfigVersion = "0.17.5"
val logbackVersion = "1.5.3"
val scalaTestVersion = "3.2.18"

scalacOptions ++= Seq(
  "-Ymacro-annotations",
  "-language:higherKinds"
)