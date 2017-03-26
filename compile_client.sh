#!/bin/bash
sbt crossJS/fastOptJS
cp js/target/scala-2.11/client-fastopt.js* js/public/
cp js/target/scala-2.11/client-jsdeps.js* js/public/
