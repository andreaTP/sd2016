lazy val root =
  project
    .in(file("."))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      scalaVersion := "2.12.0",
      scalacOptions := Seq("-feature", "-language:_", "-deprecation"),
      libraryDependencies ++= Seq(
        "eu.unicredit" %%% "akkajsactor" % "0.2.4.14",
        "eu.unicredit" %%% "akkajsactorstream" % "0.2.4.14",
        "com.lihaoyi" %%% "scalatags" % "0.6.2"
      ),
      jsDependencies +=
        "org.webjars.bower" % "diff-dom" % "2.0.3" / "diffDOM.js",
      persistLauncher in Compile := true,
      skip in packageJSDependencies := false
    )
