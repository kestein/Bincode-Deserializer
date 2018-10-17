import sbt.Keys.libraryDependencies

ThisBuild / name := "Bincode"
ThisBuild / version := "0.3"
ThisBuild / scalaVersion := "2.12.6"
ThisBuild / organization := "com.kestein"

lazy val scalatest = "org.scalatest" %% "scalatest" % "3.0.5" % "test"
lazy val jackson = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.7"

lazy val bincode = (project in file(".")).aggregate(deserializer, benchmarks)

lazy val deserializer: Project = (project in file("deserializer")).settings(
  libraryDependencies ++= Seq(
    scalatest, jackson % "test"
  ),
)

lazy val benchmarks: Project = (project in file("benchmarks")).settings(
  libraryDependencies += jackson
).dependsOn(deserializer).enablePlugins(JmhPlugin)
