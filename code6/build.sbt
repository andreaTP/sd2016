
name := "akka.js_demo"

scalaVersion in ThisBuild := "2.11.8"
scalacOptions in ThisBuild := Seq("-feature", "-language:_", "-deprecation")

lazy val root = project.in(file(".")).
  aggregate(demoJS, demoJVM)

lazy val demo = crossProject.in(file(".")).
  settings(
    name := "demo",
    fork in run := true
  ).
  jsSettings(
    libraryDependencies ++= Seq(
      "akka.js" %%% "akkaactor" % "0.1.1-SNAPSHOT",
      "org.scala-js" %%% "scalajs-dom" % "0.9.0",
      "com.lihaoyi" %%% "scalatags" % "0.5.4",
      "com.lihaoyi" %%% "upickle" % "0.4.0"
    ),
    jsDependencies += "org.webjars.bower" % "webrtc-adapter" % "0.2.9" / "adapter.js",
    persistLauncher in Compile := true,
    scalaJSStage in Global := FastOptStage,
    scalaJSUseRhino in Global := false
  )

lazy val demoJVM = demo.jvm
lazy val demoJS = demo.js

cancelable in Global := true
