
name := "backbone"

organization := "eu.unicredit"

scalaVersion := "2.11.7"

scalacOptions := Seq("-feature", "-language:_")

enablePlugins(ScalaJSPlugin)

version := "0.1-SNAPSHOT"

enablePlugins(ScalaJSPlugin)

libraryDependencies ++= Seq(
  "akka.js" %%% "akkaactor" % "0.1.0-SNAPSHOT",
  "com.lihaoyi" %%% "scalatags" % "0.5.4",
  "org.scala-js" %%% "scalajs-dom" % "0.8.1"
)

jsDependencies ++= Seq(
//  "org.webjars" % "jquery" % "2.2.0" / "jquery.js",
//  "org.webjars" % "underscorejs" % "1.8.3" / "underscore.js" dependsOn("jquery.js"),
//  "org.webjars" % "backbonejs" % "1.2.3" / "backbone.js" dependsOn("underscore.js")
)

skip in packageJSDependencies := false

persistLauncher in Compile := true

scalaJSStage in Global := FastOptStage
