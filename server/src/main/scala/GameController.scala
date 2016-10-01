import akka.actor.ActorRef
import GameController._
import concurrent.duration._

class GameController extends Handler {

  import context.dispatcher
  var position: Option[Point] = None
  var speed: Option[Point] = None
  var screenSize: Option[Point] = None

  override def onConnected(outgoing: ActorRef): Unit = {
    super.onConnected(outgoing)
    context.system.scheduler.schedule(LoopInterval, LoopInterval, self, LoopTime)
  }

  override def onEvent(outgoing: ActorRef, eventType: String, payload: String): Unit = eventType match {
    case "position" =>
      position = parsePoint(payload)
    case "speed" =>
      speed = parsePoint(payload)
    case "screenSize" =>
      screenSize = parsePoint(payload)
    case undef =>
      super.onEvent(outgoing, eventType, payload)
  }

  def onMessage = {
    case LoopTime =>
      loop()
    case undef =>
      log.error(s"Undefined message received: ${undef}")
  }

  override def onDisconnected(): Unit = {
    super.onDisconnected()
  }

  def loop(): Unit = {
    if (position.isDefined && screenSize.isDefined && this.speed.isDefined) {
      val pos = position.get
      val screen = screenSize.get
      val speed = this.speed.get

      pos.x += speed.x
      if (pos.x < 0 || pos.x > screen.x) speed.x = -speed.x
      pos.y += speed.y
      if (pos.y < 0 || pos.y > screen.y) speed.y = -speed.y

      send("position", pos.toPayload)
    }
  }

  private def parsePoint(str: String) = {
    val arr = str.split(",")
    Some(Point(arr(0).toInt, arr(1).toInt))
  }
}

object GameController {
  val LoopInterval = 10 millis
  object LoopTime

  case class Point(var x:Int, var y: Int) {
    def toPayload = s"$x,$y"
  }
}