import akka.actor.{Actor, ActorLogging, ActorRef}
import GameController._

import concurrent.duration._
import scala.util.Random

class GameController extends Actor with ActorLogging {

  import context.dispatcher
  val SIZE = 300
  var screenSize = new Point(SIZE, SIZE)

  val connections = collection.mutable.HashMap.empty[Int, ConnectionData]
  val random = new Random(123)

  override def preStart(): Unit = {
    context.system.scheduler.schedule(LoopInterval, LoopInterval, self, LoopTime)
  }

  override def receive = {
    case ConnectionHandlerImpl.Connected(handler, index) =>
      val data = new ConnectionData(handler, index)
      connections.put(index, data)
      onConnect(data)
    case ConnectionHandlerImpl.IngoingMessage(eventType, payload, index) =>
      connections.get(index) match {
        case Some(data) =>
          onEvent(data, eventType, payload)
        case None =>
          log.error("Event from undefined index = {}, eventType = {}, payload = {}", index, eventType, payload)
      }

    case ConnectionHandlerImpl.Disconnected(index) =>
      connections.remove(index) match {
        case Some(data) =>
          onDisconnect(data)
        case None =>
          log.error("Disconnection message from undefined index = {}", index)
      }
    case LoopTime =>
      loop()
  }

  def onConnect(connection: ConnectionData) = {
    connection.position = new Point(random.nextInt(screenSize.x), random.nextInt(screenSize.y))
    connection.speed = new Point(random.nextInt(10) + 1, random.nextInt(10) + 1)
    val size = random.nextInt(5) + 5
    connection.size = new Point(size, size)
    val serializedData = serializeData(connection)
    connection.send("created", serializedData)
    val gameSnapshot = connections.valuesIterator.map(serializeData).mkString(";")
    connection.send("gameSnapshot", gameSnapshot)
    connections.valuesIterator.foreach {
      c =>
        if (c != connection)
          c.send("enemyConnected", serializedData)
    }
  }

  def onEvent(connection: ConnectionData, eventType: String, payload: String) = {

  }

  def onDisconnect(connection: ConnectionData) = {
    val serializedData = serializeData(connection)
    connections.valuesIterator.foreach {
      c =>
        c.send("enemyDisconnected", serializedData)
    }
  }

  def loop(): Unit = {
    if (connections.nonEmpty) {
      connections.valuesIterator.foreach {
        c =>
          val pos = c.position
          val speed = c.speed

          pos.x += speed.x
          if (pos.x < 0 || pos.x > screenSize.x) speed.x = -speed.x
          pos.y += speed.y
          if (pos.y < 0 || pos.y > screenSize.y) speed.y = -speed.y
      }

      val tickData = connections.valuesIterator.map {
          c =>
            s"${c.index}:${c.position.toPayload}"
        }.mkString(";")

      connections.valuesIterator.foreach(_.send("tick", tickData))

    }
  }

  private def parsePoint(str: String) = {
    val arr = str.split(",")
    Some(Point(arr(0).toInt, arr(1).toInt))
  }

  private def serializeData(data:ConnectionData): String = {
    s"${data.index}:${data.position.toPayload}:${data.speed.toPayload}:${data.size.toPayload}"
  }
}

object GameController {
  val LoopInterval = 10 millis
  object LoopTime

  case class Point(var x:Int, var y: Int) {
    def toPayload = s"$x,$y"
  }

  class ConnectionData(ref: ActorRef, val index: Int) {
    var position: Point = _
    var speed: Point = _
    var size: Point = _

    def send(eventType: String, payload: String)(implicit sender: ActorRef = Actor.noSender) = {
      ref ! ConnectionHandlerImpl.OutgountMessage(eventType, payload)
    }
  }
}