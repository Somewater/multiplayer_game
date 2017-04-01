import proto.game_events.{Creature, Event}
import org.scalajs.dom.console
import ImplicitUtils._

import scala.scalajs.js

class Controller(eventClient: EventSocketClient, view: View) {
  var index: String = _
  val tmpPoint = new Point(0, 0)

  def start() = {
    eventClient.binary.on(Event.Type.CREATED, onCreated _)
    eventClient.binary.on(Event.Type.TICK, onTick _)
    eventClient.binary.on(Event.Type.GAME_SNAPSHOT, onGameSnapshot _)
    eventClient.binary.on(Event.Type.ENEMY_CONNECTED, onEnemyConnected _)
    eventClient.binary.on(Event.Type.ENEMY_DISCONNECTED, onEnemyDisconnected _)
  }

  def onInited(screenSize: Point): Unit = {

  }

  def loop() = {

  }

  def onCreated(event: Event) = {
    console.log("onCreated", event.asInstanceOf[js.Any])
    this.index = event.getCreated.getCreature.index
  }

  def onGameSnapshot(event: Event) = {
    view.removeAllCats()
    console.log("onGameSnapshot", event.asInstanceOf[js.Any])
    event.getGameSnapshot.creatures.foreach(addCreature)
  }

  def onTick(event: Event) = {
    event.getTick.creatures.foreach {
      creaturePos =>
        val index = creaturePos.index
        val position = creaturePos.getPosition
        view.catsByIndex.get(index) match {
          case Some(cat) =>
            cat.position.copyFrom(position)
          case None =>
        }
    }
  }

  def onEnemyConnected(event: Event) = {
    console.log("onEnemyConnected", event.asInstanceOf[js.Any])
    addCreature(event.getEnemyConnected.getCreature)
  }

  def onEnemyDisconnected(event: Event) = {
    console.log("onEnemyDisconnected", event.asInstanceOf[js.Any])
    val index = event.getEnemyDisconnected.getCreature.index
    view.cats.find(_.index == index) match {
      case Some(disconnected) =>
        view.removeCat(disconnected)
      case None =>
    }
  }

  private def addCreature(creature: Creature): String = {
    val index = creature.index
    val position: Point = creature.getPosition
    val speed: Point = creature.getSpeed
    val size: Point = creature.getSize
    view.addCat(new Cat(view = null, index = index, position = position, speed = speed, size = size))
    index
  }
}
