import sbt.Keys.libraryDependencies

name := "Bincode"
version := "0.3"
scalaVersion := "2.12.6"

lazy val testDeps = Seq(
    "org.scalatest" %% "scalatest" % "3.0.5" % "test",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.7" % "test",
    "com.storm-enroute" %% "scalameter" % "0.8.2"
  )

lazy val bincode = (project in file(".")).settings(
  libraryDependencies ++= testDeps,
  resolvers ++= Seq(
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"
  ),
  testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework")
)
