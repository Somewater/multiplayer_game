#!/bin/bash
sbt fastOptJS
cp target/scala-2.11/client-fastopt.js public/client-fastopt.js
cp target/scala-2.11/client-jsdeps.js public/client-jsdeps.js
