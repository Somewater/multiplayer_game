import sbt.Keys.scalaVersion

name := "Multiplayer game"

val scalaVer = "2.11.8"
scalaVersion := scalaVer

lazy val cross = crossProject.in(file(".")).
  settings(
    name := "Multiplayer game",
    version := "0.1-SNAPSHOT",
    resolvers += "Artifactory" at "https://oss.jfrog.org/artifactory/oss-snapshot-local/",
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % scalaJSVersion % "provided"
  ).
  jvmSettings(
    name := "server",
    version := "0.1-SNAPSHOT",
    scalaVersion := scalaVer,
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.11",
    libraryDependencies += "com.typesafe.akka" %% "akka-http-experimental" % "2.4.11"
  ).
  jsSettings(
    name := "client",
    version := "0.1-SNAPSHOT",
    scalaVersion := scalaVer,
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1",
    libraryDependencies += "be.doeraene" %%% "scalajs-jquery" % "0.9.1",
    libraryDependencies += "co.technius.scalajs-pixi" %%% "core" % "0.0.1-SNAPSHOT",
    jsDependencies += "org.webjars" % "pixi.js" % "3.0.7" / "pixi.js",
    jsDependencies += "org.webjars" % "jquery" % "2.1.4" / "2.1.4/jquery.js"
  )

lazy val crossJVM = cross.jvm
lazy val crossJS = cross.js
