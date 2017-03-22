import scala.scalajs.js

class Controller(eventClient: EventSocketClient, view: View) {
  var index: String = _
  val tmpPoint = new Point(0, 0)

  def start() = {
    eventClient.on("created", onCreated _)
    eventClient.on("gameSnapshot", onGameSnapshot _)
    eventClient.on("enemyConnected", onEnemyConnected _)
    eventClient.on("enemyDisconnected", onEnemyDisconnected _)
  }

  def onInited(screenSize: Point): Unit = {

  }

  def loop() = {

  }

  def onCreated(evType: String, payload: String) = {
    index = addCreature(payload)
  }

  def onGameSnapshot(evType: String, payload: String) = {
    payload.split(";").foreach {
      part =>
        val arr = part.split(":")
        val index = arr(0)
        var position = Point.fromPayload(arr(1), tmpPoint)
        view.catsByIndex.get(index) match {
          case Some(cat) =>
            cat.position.copyFrom(position)
          case None =>
        }
    }
  }

  def onEnemyConnected(evType: String, payload: String) = {
    addCreature(payload)
  }

  def onEnemyDisconnected(evType: String, payload: String) = {
    val (index, _, _, _) = deseriaizedData(payload)
    view.cats.find(_.index == index) match {
      case Some(disconnected) =>
        view.removeCat(disconnected)
      case None =>
    }
  }

  private def addCreature(payload: String): String = {
    val (index, position, speed, size) = deseriaizedData(payload)
    view.addCat(new Cat(view = null, index = index, position = position, speed = speed, size = size))
    index
  }

  private def deseriaizedData(payload: String): (String, Point, Point, Point) = {
    val parts = payload.split(":")
    (parts(0),
      Point.fromPayload(parts(1)),
      Point.fromPayload(parts(2)),
      Point.fromPayload(parts(3)))
  }
}
