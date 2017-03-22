import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable}
import akka.http.scaladsl.model.ws.TextMessage
import scala.concurrent.duration._

/**
  * Help handle connection
  */
abstract class ConnectionHandler extends Actor with ActorLogging {

  import ConnectionHandler._
  import context.dispatcher

  var pingSchedule: Option[Cancellable] = None
  var outgoing: Option[ActorRef] = None
  val outgoingEvents = collection.mutable.ArrayBuffer.empty[(String, String)]

  def onConnected(outgoing: ActorRef): Unit

  def onDisconnected(): Unit

  def onEvent(outgoing: ActorRef, eventType: String, payload: String)

  def onMessage: Receive

  override def postStop(): Unit = {
    super.postStop()
    pingSchedule.foreach(_.cancel())
    log.warning("Actor stopped")
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
    case TextMessage.Strict(text) =>
      val idx = text.indexOf(":")
      if (idx == -1 || idx == 0) {
        log.error(s"Wrong message = ${text}")
      } else {
        val eventType = text.substring(0, idx)
        val payload = text.substring(idx + 1)
        eventType match {
          case "pong" =>
            val duration = System.currentTimeMillis() - payload.toLong
            log.info(s"Ping response during ${duration} ms")
          case _ =>
            onEvent(outgoing, eventType, payload)
        }
      }

    case TextMessage.Streamed(text) =>
      log.error(s"Unhandled streamed msg = ${text}")

    case PingTime =>
      outgoing ! TextMessage(s"ping:${System.currentTimeMillis()}")

    case msg =>
      onMessage(msg)
  }

  protected def send(eventType: String, payload: String) = {
    outgoing match {
      case Some(conn) =>
        conn ! TextMessage(s"${eventType}:${payload}")
      case None =>
        outgoingEvents.append()
    }
  }

  protected def processOutgoingEvents(): Unit = {
    val conn = outgoing.get
    for ((eventType, payload) <- outgoingEvents.iterator) {
      conn ! TextMessage(s"${eventType}:${payload}")
    }
    outgoingEvents.clear()
  }
}

object ConnectionHandler {
  val PingInterval = 5 second // must be less then 60 sec

  private object PingTime
}