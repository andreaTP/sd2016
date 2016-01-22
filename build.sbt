
name := "sd2016"

organization := "eu.unicredit"

scalaVersion := "2.11.7"

scalacOptions := Seq("-feature", "-language:_")

enablePlugins(ScalaJSPlugin)

version := "0.1-SNAPSHOT"

enablePlugins(ScalaJSPlugin)

libraryDependencies ++= Seq(
  "akka.js" %%% "akkaactor" % "0.1.0-SNAPSHOT",
  "org.scala-js" %%% "scalajs-dom" % "0.8.1",
  "com.lihaoyi" %%% "scalatags" % "0.5.3"
)

jsDependencies ++= Seq(
  "org.webjars.bower" % "webrtc-adapter" % "0.2.5" / "adapter.js"
)

persistLauncher in Compile := true

scalaJSStage in Global := FastOptStage
