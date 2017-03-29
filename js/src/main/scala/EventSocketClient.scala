import java.io.InputStream
import java.nio.ByteBuffer

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom
import org.scalajs.dom.WebSocket
import org.scalajs.dom.raw.Blob
import proto.game_events.Event

import scala.collection.mutable
import scala.scalajs.js.Array
import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}
import scala.scalajs.js.typedarray.TypedArrayBufferOps._
import scala.scalajs.js.JSConverters._

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
class EventSocketClient(host: String, port: Int, verbose: Boolean = true, pingPong: Boolean = true) {

  val binary = new EventDispatcher[Event.Type, Event]
  val text = new EventDispatcher[String, String]
  val state = new EventDispatcher[Unit, Int]
  private val textSender = new BufferedSender[(String, String)]()(sendText _)
  private val binarySender = new BufferedSender[Event]()(sendBinary _)

  private var socket: WebSocket = null

  if (pingPong) text.on("ping", pingHandler _)

  def start(): Unit = {
    if (status == WebSocket.OPEN) {
      if (verbose)
        dom.console.error("Already connected")
      return
    }

    if (status == WebSocket.CONNECTING) {
      if (verbose)
        dom.console.error("Already connecting...")
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

  def send(event: Event) = {
    binarySender.send(event)
  }

  def send(evType: String, msg: String) = {
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
    if (verbose) dom.console.error(ev)
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
      dom.console.error("Can't handle blob websocket message")
    } else {
      val data = if (event.data == null) "" else event.data.toString
      val idx = data.indexOf(":")
      if (idx == -1 || idx == 0) {
        if (verbose)
          dom.console.error(event)
      } else {
        val evType = data.substring(0, idx)
        val payload = data.substring(idx + 1)
        if (verbose)
          dom.console.log(s"[WS] ${evType}:${payload}")
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
    dom.console.log(s"Ping duration ${now - msg.toLong}")
    send("pong", msg)
  }
}
