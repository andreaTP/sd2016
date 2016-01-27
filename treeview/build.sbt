
name := "treeview"

organization := "eu.unicredit"

scalaVersion := "2.11.7"

scalacOptions := Seq("-feature")

enablePlugins(ScalaJSPlugin)

version := "0.1-SNAPSHOT"

enablePlugins(ScalaJSPlugin)

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.8.1",
  "eu.unicredit" %%% "paths-scala-js" % "0.4.2"
)

jsDependencies ++= Seq()

persistLauncher in Compile := true

scalaJSStage in Global := FastOptStage
