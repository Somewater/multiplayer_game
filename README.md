### Overview
Multiplayer game draft created using only `Scala` language. Features:
* ScalaJS for client code
* Akka IO for server code, websockets
* PixiJS for fast graphics rendering

### Installation:
* Create sym link to client part as `server/client` -> `path to client subproject`
* Install `nodejs`, create executable named `node` pointed to `nodejs`
* Install source map plugin using `npm install source-map-support` for comfortable debugging
* Compile client using `client/compile.sh` script
* Run server `cd server && sbt run`