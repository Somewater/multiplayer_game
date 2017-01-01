enablePlugins(ScalaJSPlugin)

name := "client"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += "Artifactory" at "https://oss.jfrog.org/artifactory/oss-snapshot-local/"

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.9.1",
  "be.doeraene" %%% "scalajs-jquery" % "0.9.1",
  "co.technius.scalajs-pixi" %%% "core" % "0.0.1-SNAPSHOT"
)

jsDependencies += "org.webjars" % "pixi.js" % "3.0.7" / "pixi.js"
jsDependencies += "org.webjars" % "jquery" % "2.1.4" / "2.1.4/jquery.js"
