import Dependencies._

lazy val CoyaBackendChallenge = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.coya",
      scalaVersion := "2.12.7",
      version      := "0.1.1-SNAPSHOT"
    )),
    libraryDependencies ++= Seq(
      "org.typelevel"  %% "squants"  % "1.4.0",
      scalaTest % Test
    )
  )
