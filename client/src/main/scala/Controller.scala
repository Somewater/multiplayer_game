import scala.scalajs.js

class Controller(eventClient: EventSocketClient, view: View) {
  def start() = {
    eventClient.on("position", onPositionEvent _)
  }

  def onInited(position: Point,
               speed: Point,
               screenSize: Point): Unit = {
    eventClient.send("position", Point.toPayload(position))
    eventClient.send("speed", Point.toPayload(speed))
    eventClient.send("screenSize", Point.toPayload(screenSize))
  }

  def loop() = {

  }

  def onPositionEvent(evType: String, payload: String) = {
    val p = Point.fromPayload(payload)
    val cat = view.cats.apply(0)
    if (cat != null) {
      cat.view.x = p.x
      cat.view.y = p.y
    }
  }
}
