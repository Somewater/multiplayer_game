import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom
import org.scalajs.dom.console
import org.scalajs.dom.WebSocket
import org.scalajs.dom.raw.Blob
import proto.game_events.Event

import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}
import scala.scalajs.js.JSConverters._
import scala.util.{Failure, Success, Try}

/**
  * Socket client interface with events dispatching
  * Usage:
  * <pre>
  * var client = new EventSocketClient("0.0.0.0", 81)
  * client.addStateListener(function(newState){
  *   if (newState = WebSocket.OPEN)
  *     console.log("Connected")
  * });
  * client.on("hello", function(eventType, msg){
  *   console.log("Someone congrat us with: " + msg);
  *
  * })
  * </pre>
  */
@JSExport
class EventSocketClient(host: String, port: Int, verbose: Boolean = false, pingPong: Boolean = true) {

  val binary = new EventDispatcher[Event.Type, Event]
  val text = new EventDispatcher[String, String]
  val state = new EventDispatcher[Unit, Int]
  private val textSender = new BufferedSender[(String, String)]()(sendText _)
  private val binarySender = new BufferedSender[Event]()(sendBinary _)

  protected var socket: WebSocket = null

  if (pingPong) text.on("ping", pingHandler _)

  def start(): Unit = {
    if (status == WebSocket.OPEN) {
      if (verbose)
        console.error("Already connected")
      return
    }

    if (status == WebSocket.CONNECTING) {
      if (verbose)
        console.error("Already connecting...")
      return
    }

    if (socket != null) {
      socket.close()
      socket = null
    }

    socket = new dom.WebSocket(s"ws://${this.host}:${this.port}/ws")
    socket.binaryType = "arraybuffer"
    state.dispatch((), status)
    socket.onopen = onOpen _
    socket.onclose = onClose _
    socket.onmessage = onMessage _
    socket.onerror = onError _
  }

  def reconnect() = {
    if (status != WebSocket.OPEN && status != WebSocket.CONNECTING) {
      start()
    }
  }

  def status: Int =
    if (socket == null) WebSocket.CLOSED else socket.readyState

  def send(event: Event): Boolean = {
    binarySender.send(event)
  }

  def send(evType: String, msg: String): Boolean = {
    textSender.send(evType -> msg)
  }

  private def onOpen(ev: dom.Event) = {
    state.dispatch((), status)
    binarySender.open()
    textSender.open()
  }

  private def onClose(ev: dom.Event) = {
    state.dispatch((), status)
    binarySender.close()
    textSender.close()
  }

  private def onError(ev: dom.Event) = {
    if (verbose) console.error(ev)
  }

  private def onMessage(event: dom.MessageEvent) = {
    if (event.data.isInstanceOf[ArrayBuffer]) {
      val view = new Uint8Array(event.data.asInstanceOf[ArrayBuffer])
      val len = view.length
      val array = new scala.Array[Byte](len)
      var i = 0
      while (i < len) {
        array(i) = view.get(i).asInstanceOf[Byte]
        i += 1
      }
      val binaryEvent = Event.parseFrom(array)
      binary.dispatch(binaryEvent.`type`, binaryEvent)
    } else if (event.data.isInstanceOf[Blob]) {
      console.error("Can't handle blob websocket message")
    } else {
      val data = if (event.data == null) "" else event.data.toString
      val idx = data.indexOf(":")
      if (idx == -1 || idx == 0) {
        if (verbose)
          console.error(event)
      } else {
        val evType = data.substring(0, idx)
        val payload = data.substring(idx + 1)
        if (verbose)
          console.log(s"[WS] ${evType}:${payload}")
        text.dispatch(evType, payload)
      }
    }
  }

  private def sendBinary(event: Event) = {
    val array = event.toByteString.toByteArray.toJSArray
    val view = new Uint8Array(array)
    socket.send(view.buffer)
  }

  private def sendText(pair: (String, String)) = {
    socket.send(s"${pair._1}:${pair._2}")
  }

  private def pingHandler(msg: String): Unit = {
    val now = System.currentTimeMillis()
    if (verbose) console.log(s"Ping duration ${now - msg.toLong}")
    send("pong", msg)
  }
}

trait Reconnect { this: EventSocketClient =>
  private var running = false
  private var reconnectAttempts = 0
  private var maxReconnectAttempts = 100
  private var onFatalError: (String => Any) = printToConsole _
  private var reconnectIntervalMs = 0
  private var reconnectMultiplier = 1.0D

  def startWithReconnect(maxReconnectAttempts: Int = Int.MaxValue,
                         reconnectIntervalMs: Int = 0,
                         reconnectMultiplier: Double = 1.0D,
                         onFatalError: String => Any = printToConsole _): Unit = {
    this.maxReconnectAttempts = maxReconnectAttempts
    this.onFatalError = onFatalError
    this.reconnectIntervalMs = reconnectIntervalMs
    this.reconnectMultiplier = reconnectMultiplier
    running = true
    state.on((), onStateChanged _)
    tryToStart()
  }

  def stop(): Unit = {
    running = false
    this.socket.close()
  }

  private def onStateChanged(state: Int) = {
    if (running) {
      state match {
        case WebSocket.CONNECTING =>
        case WebSocket.OPEN =>
          reconnectAttempts = 0
        case WebSocket.CLOSING =>
        case WebSocket.CLOSED =>
          tryToReconnect()
        case another =>
          console.error(s"Undefined WebSocket state = ${state}")
      }
    }
  }

  private def tryToReconnect(forceTimeout: Option[Int] = None): Unit = {
    if (reconnectAttempts < maxReconnectAttempts) {
      reconnectAttempts += 1
      val interval = forceTimeout.map(t => math.max(reconnectIntervalMs, t)).getOrElse(reconnectIntervalMs)
      reconnectIntervalMs = (reconnectIntervalMs * reconnectMultiplier).toInt
      if (interval == 0) {
        tryToStart()
      } else {
        val timeout = scala.scalajs.js.timers.setTimeout(interval) {
          tryToStart()
        }
      }
    } else {
      onFatalError("Reconnection limit exceeded, WebSocket connection failed")
    }
  }

  private def tryToStart() = {
    Try(start()) match {
      case Failure(ex) =>
        console.error("WebSocket connection error", ex.asInstanceOf[js.Any])
        tryToReconnect(forceTimeout = Some(1000))
      case Success(_) =>
    }
  }

  private def printToConsole(msg: String): Unit = console.log(msg)
}
