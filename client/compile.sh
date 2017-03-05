#!/bin/bash
sbt fastOptJS
cp target/scala-2.11/client-fastopt.js* public/
cp target/scala-2.11/client-jsdeps.js* public/
