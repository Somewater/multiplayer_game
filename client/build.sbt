enablePlugins(ScalaJSPlugin)

name := "client"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += "Artifactory" at "https://oss.jfrog.org/artifactory/oss-snapshot-local/",

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.9.1",
  "co.technius.scalajs-pixi" %%% "core" % "0.0.1-SNAPSHOT"
)

