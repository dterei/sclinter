name := "scala-linter"

version := "0.1"

scalaVersion := "2.12.3"

libraryDependencies ++= Seq(
  "com.beachape" %% "enumeratum" % "1.5.12",
  "com.beachape" %% "enumeratum-play-json" % "1.5.12-2.6.0-M7",
  "org.scalameta" %% "scalameta" % "2.0.0-RC1",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "com.typesafe.play" %% "play-json" % "2.6.6"
)

testOptions in Test +=
  Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports")
