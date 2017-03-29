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
import akka.stream.{ActorMaterializer, OverflowStrategy, scaladsl}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.config.ConfigFactory

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
    implicit val conf = ConfigFactory.load()

    var actorCounter = new AtomicInteger(0)

    def webSocketFlow(props: Int => Props): Flow[Message, Message, _] = {
      val idx = actorCounter.incrementAndGet
      val actor = system.actorOf(props(idx), name = s"Handler_${idx}")


      val incomingMessages: Sink[Message, NotUsed] =
        Flow[Message].map {
          case msg: TextMessage => msg
        }.to(Sink.actorRef[TextMessage](actor, PoisonPill))

      val outgoingMessages: Source[Message, NotUsed] =
        Source.actorRef[Message](10, OverflowStrategy.fail)
          .mapMaterializedValue { outActor =>
            // give the user actor a way to send messages out
            actor ! Connected(outActor)
            NotUsed
          }
      // then combine both to a flow
      Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
    }

    def requestHandler(controller: ActorRef)(request: HttpRequest): HttpResponse = request match {
      case req @ HttpRequest(GET, Uri.Path("/ws"), _, _, _) =>
        req.header[UpgradeToWebSocket] match {
          case Some(upgrade) =>
            upgrade.handleMessages(webSocketFlow(idx => Props(new ConnectionHandlerImpl(controller, idx))))
          case None =>
            HttpResponse(400, entity = "Not a valid websocket request!")
        }

      case req @ HttpRequest(GET, Uri.Path(filepath0), _, _, _) =>
        val filepath = if (filepath0 == "/") "/index.html" else filepath0
        val file = new File(conf.getString("game.client.public.path") + filepath)

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

    val controller = system.actorOf(Props(new GameController))
    val bind = Http().bindAndHandleSync(requestHandler(controller), interface = "0.0.0.0", port = 61618)

    println(s"Server online at http://localhost:61618/\nPress RETURN to stop...")
    StdIn.readLine()

    import system.dispatcher
    bind.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }

}
