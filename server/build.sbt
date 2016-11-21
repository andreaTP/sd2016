lazy val root =
  project
    .in(file("."))
    .enablePlugins(ScalaJSPlugin)
    .settings(reformatOnCompileSettings)
    .settings(
      scalaVersion := "2.12.0",
      libraryDependencies +=
        "eu.unicredit" %%% "akkajsactor" % "0.2.4.12",
      fork in run := true,
      cancelable in Global := true
    )
