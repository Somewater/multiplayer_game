import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.TextMessage
import ConnectionHandlerImpl._
import proto.game_events.Event

/**
  * Integration with game controller
  */
class ConnectionHandlerImpl(controller: ActorRef, index: Int) extends ConnectionHandler {

  import context.dispatcher

  override def onConnected(outgoing: ActorRef): Unit = {
    controller ! Connected(self, index)
  }

  override def onDisconnected(): Unit = {
    controller ! Disconnected(index)
  }

  override def onEvent(outgoing: ActorRef, event: Event): Unit = {
    controller ! IngoingMessage(event, index)
  }

  override def onMessage: Receive = {
    case OutgountMessage(event) =>
      send(event)
  }
}

object ConnectionHandlerImpl {
  sealed trait WsEvent {
    val index: Int
  }
  case class Connected(handler: ActorRef, val index: Int) extends WsEvent
  case class IngoingMessage(event: Event, val index: Int) extends WsEvent
  case class OutgountMessage(event: Event)
  case class Disconnected(val index: Int) extends WsEvent
}