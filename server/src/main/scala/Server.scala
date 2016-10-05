import java.io.File
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger

import akka.NotUsed
import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.server.Directives
import akka.stream.{ActorMaterializer, scaladsl, OverflowStrategy}
import akka.stream.scaladsl.{Sink, Source, Flow}


import scala.concurrent.Future
import scala.io.StdIn
import scala.concurrent.duration._

/**
  * Server entry point
  */
object Server {

  case class Connected(connection: ActorRef)

  def main(args: Array[String]) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    var actorCounter = new AtomicInteger(0)

    def webSocketFlow(props: Props): Flow[Message, Message, _] = {
      val actor = system.actorOf(props, name = s"Handler_${actorCounter.incrementAndGet}")


      val incomingMessages: Sink[Message, NotUsed] =
        Flow[Message].map {
          case msg: TextMessage => msg
        }.to(Sink.actorRef[TextMessage](actor, PoisonPill))

      val outgoingMessages: Source[Message, NotUsed] =
        Source.actorRef[TextMessage](10, OverflowStrategy.fail)
          .mapMaterializedValue { outActor =>
            // give the user actor a way to send messages out
            actor ! Connected(outActor)
            NotUsed
          }.map(
          // transform domain message to web socket message
          (msg: TextMessage) =>
            msg
        )
      // then combine both to a flow
      Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
    }

    def requestHandler(request: HttpRequest): HttpResponse = request match {
      case req @ HttpRequest(GET, Uri.Path("/ws"), _, _, _) =>
        val h = req.header[UpgradeToWebSocket]
        req.header[UpgradeToWebSocket] match {
          case Some(upgrade) =>
            upgrade.handleMessages(webSocketFlow(Props(new GameController)))
          case None =>
            HttpResponse(400, entity = "Not a valid websocket request!")
        }

      case req @ HttpRequest(GET, Uri.Path(filepath0), _, _, _) =>
        val filepath = if (filepath0 == "/") "/index.html" else filepath0
        val file = new File(System.getProperty("user.dir") + "/client/public" + filepath)

        val contentType =
          if (filepath.endsWith(".html") || filepath.endsWith(".htm"))
            ContentTypes.`text/html(UTF-8)`
          else if (filepath.endsWith(".js"))
            ContentTypes.`application/json`
          else
            ContentTypes.`text/plain(UTF-8)`

        if (file.exists())
          HttpResponse(entity = HttpEntity.fromPath(contentType, file.toPath))
        else
          HttpResponse(StatusCodes.NotFound)

      case r: HttpRequest =>
        r.discardEntityBytes() // important to drain incoming HTTP Entity stream
        HttpResponse(404, entity = "Unknown resource!")
    }

    val bind = Http().bindAndHandleSync(requestHandler, interface = "0.0.0.0", port = 61618)

    println(s"Server online at http://localhost:61618/\nPress RETURN to stop...")
    StdIn.readLine()

    import system.dispatcher
    bind.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }

}

abstract class Handler extends Actor with ActorLogging {

  import Handler._
  import context.dispatcher

  var pingSchedule: Option[Cancellable] = None
  var outgoing: Option[ActorRef] = None
  val outgoingEvents = collection.mutable.ArrayBuffer.empty[(String, String)]

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
        onEvent(outgoing, eventType, payload)
      }

    case TextMessage.Streamed(text) =>
      log.error(s"Unhandled streamed msg = ${text}")

    case PingTime =>
      outgoing ! TextMessage(s"ping:${System.currentTimeMillis()}")

    case msg =>
      onMessage(msg)
  }

  def onEvent(outgoing: ActorRef, eventType: String, payload: String) = eventType match {
    case "pong" =>
      val duration = System.currentTimeMillis() - payload.toLong
      log.info(s"Ping response during ${duration} ms")

    case undefined =>
      log.error(s"Undefined event eventType=${eventType}, payload=${payload}")
  }

  def onConnected(outgoing: ActorRef): Unit = {

  }

  def onDisconnected(): Unit = {

  }

  def onMessage: Receive

  def send(eventType: String, payload: String) = {
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

object Handler {
  val PingInterval = 5 second // must be less then 60 sec

  object PingTime
}