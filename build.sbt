
lazy val sclinter =
  crossProject
    .crossType(CrossType.Full)
    .in(file("."))
    .settings(
      name := "sclinter",
      version := "0.1.3",
      scalaVersion := "2.12.5",
      jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv(),

      libraryDependencies ++= Seq(
        "com.beachape" %%% "enumeratum" % "1.5.12",
        "com.lihaoyi" %%% "upickle" % "0.6.2",
        "org.scalameta" %%% "scalameta" % "3.7.0",
        "org.scalatest" %%% "scalatest" % "3.0.4" % "test",
      ),

      scalacOptions ++= Seq(
        "-deprecation",
        "-encoding", "utf-8",
        "-explaintypes",
        "-feature",
        "-language:existentials",
        "-unchecked",
        "-Xcheckinit",
        "-Xfatal-warnings",
        "-Xlint:adapted-args",
        "-Xlint:by-name-right-associative",
        "-Xlint:constant",
        "-Xlint:delayedinit-select",
        "-Xlint:doc-detached",
        "-Xlint:inaccessible",
        "-Xlint:infer-any",
        "-Xlint:missing-interpolator",
        "-Xlint:nullary-override",
        "-Xlint:nullary-unit",
        "-Xlint:option-implicit",
        "-Xlint:package-object-classes",
        "-Xlint:poly-implicit-overload",
        "-Xlint:private-shadow",
        "-Xlint:stars-align",
        "-Xlint:type-parameter-shadow",
        "-Xlint:unsound-match",
        "-Yno-adapted-args",
        "-Ypartial-unification",
        "-Ywarn-extra-implicit",
        "-Ywarn-inaccessible",
        "-Ywarn-infer-any",
        "-Ywarn-nullary-override",
        "-Ywarn-nullary-unit",
        "-Ywarn-numeric-widen",
        "-Ywarn-unused:implicits",
        "-Ywarn-unused:imports",
        "-Ywarn-unused:locals",
        "-Ywarn-unused:params",
        "-Ywarn-unused:privates",
        "-Ywarn-value-discard"
      ),

      scalacOptions in Test --= Seq(
        "-Ywarn-value-discard"
      ),

      testOptions in Test ++= Seq(
        Tests.Argument("-oDF"),
      ),

      assemblyJarName in assembly := s"${name.value}.jar",

      // Skip tests during assembly
      test in assembly := {},
    )
    .jvmSettings(
      libraryDependencies ++= Seq(
        "org.scala-js" %% "scalajs-stubs" % scalaJSVersion % "provided",
      ),

      testOptions in Test ++= Seq(
        Tests.Argument(
          TestFrameworks.ScalaTest, "-u", "jvm/target/test-reports"),
      ),
    )
    .jsSettings(
      // Uncomment the following if we ever have a JS main module
      // scalaJSUseMainModuleInitializer := true,
      scalaJSOutputWrapper := (
        "global.require = require;",
        "LinterApp.lint.apply(LinterApp, process.argv.slice(2));"
      ),
    )

lazy val sclinterJVM = sclinter.jvm
lazy val sclinterJS = sclinter.js
