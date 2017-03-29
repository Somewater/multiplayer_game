import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable}
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.util.ByteString
import proto.game_events.Event

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
  * Help handle connection
  */
abstract class ConnectionHandler extends Actor with ActorLogging {

  import ConnectionHandler._
  import context.dispatcher

  var pingSchedule: Option[Cancellable] = None
  var outgoing: Option[ActorRef] = None
  val outgoingEvents = collection.mutable.ArrayBuffer.empty[Event]

  def onConnected(outgoing: ActorRef): Unit

  def onDisconnected(): Unit

  def onEvent(outgoing: ActorRef, event: Event)

  def onMessage: Receive

  override def postStop(): Unit = {
    super.postStop()
    pingSchedule.foreach(_.cancel())
    log.warning("Connection handler stopped")
    onDisconnected()
  }

  def receive = {
    case Server.Connected(outgoing) =>
      log.info("Client connected")
      this.outgoing = Some(outgoing)
      processOutgoingEvents()
      context.watch(outgoing)
      context.become(connected(outgoing))
      pingSchedule = Some(context.system.scheduler.schedule(PingInterval, PingInterval, self, PingTime))
      onConnected(outgoing)
  }

  def connected(outgoing: ActorRef): Receive = {
    case BinaryMessage.Strict(bytes) =>
      Try { Event.parseFrom(bytes.toArray) } match {
        case Success(event) =>
          onEvent(outgoing, event)
        case Failure(ex) =>
          log.error(ex, s"Wrong binary message")
      }

    case TextMessage.Strict(text) =>
      val idx = text.indexOf(":")
      if (idx == -1 || idx == 0) {
        log.error(s"Wrong text message = ${text}")
      } else {
        val eventType = text.substring(0, idx)
        val payload = text.substring(idx + 1)
        eventType match {
          case "pong" =>
            val duration = System.currentTimeMillis() - payload.toLong
            log.info(s"Ping response during ${duration} ms")
          case _ =>
            log.error(s"Unhandled text message = ${text}")
        }
      }

    case TextMessage.Streamed(text) =>
      log.error(s"Unhandled streamed msg = ${text}")

    case PingTime =>
      outgoing ! TextMessage(s"ping:${System.currentTimeMillis()}")

    case msg =>
      onMessage(msg)
  }

  protected def send(event: Event) = {
    outgoing match {
      case Some(conn) =>
        conn ! BinaryMessage(ByteString(event.toByteArray))
      case None =>
        outgoingEvents.append(event)
    }
  }

  protected def processOutgoingEvents(): Unit = {
    val conn = outgoing.get
    for (event <- outgoingEvents.iterator) {
      conn ! BinaryMessage(ByteString(event.toByteArray))
    }
    outgoingEvents.clear()
  }
}

object ConnectionHandler {
  val PingInterval = 5 second // must be less then 60 sec

  private object PingTime
}