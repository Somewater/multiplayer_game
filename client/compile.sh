#!/bin/bash
sbt fastOptJS
cp target/scala-2.11/client-fastopt.js public/client-fastopt.js
