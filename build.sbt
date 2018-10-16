import sbt.Keys.libraryDependencies

ThisBuild / name := "Bincode"
ThisBuild / version := "0.3"
ThisBuild / scalaVersion := "2.12.6"
ThisBuild / organization := "com.kestein"

lazy val scalatest = "org.scalatest" %% "scalatest" % "3.0.5" % "test"
lazy val jackson = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.7"
lazy val scalameter = "com.storm-enroute" %% "scalameter" % "0.8.2"

lazy val bincode = (project in file(".")).aggregate(deserializer, benchmarks)

lazy val deserializer: Project = (project in file("deserializer")).settings(
  libraryDependencies ++= Seq(
    scalatest, jackson % "test"
  ),
)

lazy val benchmarks: Project = (project in file("benchmarks")).settings(
  libraryDependencies ++= Seq(
    jackson, scalameter
  ),
  resolvers ++= Seq(
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"
  ),
  testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework")
).dependsOn(deserializer)
