import akka.actor.{Actor, ActorLogging, ActorRef}
import GameController._
import proto.game_events
import proto.game_events.Tick.CreaturePosition
import proto.game_events._

import concurrent.duration._
import scala.util.Random

class GameController extends Actor with ActorLogging {

  import context.dispatcher
  val SIZE = 300
  var screenSize = new Point(SIZE, SIZE)

  val connections = collection.mutable.HashMap.empty[Int, ConnectionData]
  val random = new Random(123)

  implicit def option[V](v: V): Option[V] = Some(v)

  override def preStart(): Unit = {
    context.system.scheduler.schedule(LoopInterval, LoopInterval, self, LoopTime)
  }

  override def receive = {
    case ConnectionHandlerImpl.Connected(handler, index) =>
      val data = new ConnectionData(handler, index)
      connections.put(index, data)
      onConnect(data)
    case ConnectionHandlerImpl.IngoingMessage(event, index) =>
      connections.get(index) match {
        case Some(data) =>
          onEvent(data, event)
        case None =>
          log.error("Event from undefined index = {}, eventType = {}", index, event.`type`.name)
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
    connection.position = new Point(random.nextInt(screenSize.x.toInt), random.nextInt(screenSize.y.toInt))
    connection.speed = new Point(random.nextInt(10) + 1, random.nextInt(10) + 1)
    val size = random.nextInt(5) + 5
    connection.size = new Point(size, size)
    val serializedData = serializeData(connection)
    connection.send(Event(`type` = Event.Type.CREATED,
      value = Event.Value.Created(Created(creature = toCreature(connection)))))
    connection.send(Event(`type` = Event.Type.GAME_SNAPSHOT,
      value = Event.Value.GameSnapshot(GameSnapshot(creatures = connections.valuesIterator.map(toCreature).toSeq))))
    connections.valuesIterator.foreach {
      c =>
        if (c != connection)
          c.send(Event(`type` = Event.Type.ENEMY_CONNECTED,
            value = Event.Value.EnemyConnected(EnemyConnected(creature = toCreature(connection)))))
    }
  }

  def onEvent(connection: ConnectionData, event: Event) = {

  }

  def onDisconnect(connection: ConnectionData) = {
    val serializedData = serializeData(connection)
    connections.valuesIterator.foreach {
      c =>
        connection.send(Event(`type` = Event.Type.ENEMY_DISCONNECTED,
          value = Event.Value.EnemyDisconnected(EnemyDisonnected(creature = toCreature(connection)))))
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

      connections.valuesIterator.foreach(_.send(Event(`type` = Event.Type.TICK,
        value = Event.Value.Tick(Tick(creatures = connections.valuesIterator.map(toCreaturePosition).toSeq)))))

    }
  }

  private def parsePoint(str: String) = {
    val arr = str.split(",")
    Some(new Point(arr(0).toDouble, arr(1).toDouble))
  }

  private def serializeData(data:ConnectionData): String = {
    s"${data.index}:${data.position.toPayload}:${data.speed.toPayload}:${data.size.toPayload}"
  }

  private def toCreature(connection: ConnectionData): Creature = {
    Creature(
      index = connection.index.toString,
      position = toPoint(connection.position),
      speed = toPoint(connection.speed),
      size = toPoint(connection.size))
  }

  private def toCreaturePosition(connection: ConnectionData): CreaturePosition = {
    CreaturePosition(index = connection.index.toString, position = toPoint(connection.position))
  }

  implicit private def toPoint(p: Point) = game_events.Point(p.x.toInt, p.y.toInt)
}

object GameController {
  val LoopInterval = 10 millis
  object LoopTime

  class ConnectionData(ref: ActorRef, val index: Int) {
    var position: Point = _
    var speed: Point = _
    var size: Point = _

    def send(event: Event)(implicit sender: ActorRef = Actor.noSender) = {
      ref ! ConnectionHandlerImpl.OutgountMessage(event)
    }
  }
}