import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.TextMessage
import ConnectionHandlerImpl._

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

  override def onEvent(outgoing: ActorRef, eventType: String, payload: String): Unit = {
    controller ! IngoingMessage(eventType, payload, index)
  }

  override def onMessage: Receive = {
    case OutgountMessage(eventType, payload) =>
      send(eventType, payload)
  }
}

object ConnectionHandlerImpl {
  sealed trait WsEvent {
    val index: Int
  }
  case class Connected(handler: ActorRef, val index: Int) extends WsEvent
  case class IngoingMessage(eventType: String, payload: String, val index: Int) extends WsEvent
  case class OutgountMessage(eventType: String, payload: String)
  case class Disconnected(val index: Int) extends WsEvent
}