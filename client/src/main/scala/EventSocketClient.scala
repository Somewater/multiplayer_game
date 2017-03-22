import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom
import org.scalajs.dom.WebSocket

import scala.collection.mutable
import scala.scalajs.js.Array

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

  type MsgListener = js.Function2[String, String, Any]
  type StateListener = js.Function1[Int, Unit]

  private var stateListeners = new js.Array[StateListener]
  private var messageListeners = mutable.Map.empty[String, js.Array[MsgListener]]
  private var eventsQueue = new js.Array[js.Array[String]]
  private var socket: WebSocket = null

  if (pingPong) on("ping", pingHandler _)


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
    dispatchStatus()
    socket.onopen = (ev: dom.Event) => {
      dispatchStatus()
      processOutgointEvents()
    }
    socket.onclose = (ev: dom.Event) => dispatchStatus()
    socket.onmessage = onMessage _
    socket.onerror = (ev: dom.Event) => if (verbose) dom.console.error(ev)
  }

  def reconnect() = {
    if (status != WebSocket.OPEN && status != WebSocket.CONNECTING) {
      start()
    }
  }

  def status: Int =
    if (socket == null) WebSocket.CLOSED else socket.readyState

  def on(eventType: String, listener: MsgListener) = {
    messageListeners.getOrElseUpdate(eventType, new js.Array[MsgListener]()).push(listener)
  }

  def send(evType: String, msg: String) = {
    if (status == WebSocket.OPEN) {
      _send(evType, msg)
    } else {
      eventsQueue.push(js.Array(evType, msg))
    }
  }

  def addStateListener(listener: StateListener) = stateListeners.push(listener)

  private def onMessage(event: dom.MessageEvent) = {
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
      dispatch(evType, payload)
    }
  }

  private def dispatchStatus() = {
    val status = this.status
    if (verbose)
      dom.console.log(s"WS status: ${status}")
    stateListeners.foreach(cb => cb(status))
  }

  private def _send(evType: String, msg: String) = {
    val str = s"${evType}:${msg}"
    if (verbose)
      dom.console.log(s"[SEND] $str")
    socket.send(str)
  }

  private def dispatch(evType: String, msg: String) = {
    messageListeners.get(evType) match {
      case Some(listeners) =>
        listeners.foreach(cb => cb(evType, msg))
      case None =>
    }
  }

  private def pingHandler(evType: String, msg: String): Unit = {
    val now = System.currentTimeMillis()
    dom.console.log(s"Ping duration ${now - msg.toLong}")
    send("pong", msg)
  }

  private def processOutgointEvents() = {
    if (eventsQueue.length > 0) {
      eventsQueue.foreach(
        ev =>
          _send(ev(0), ev(1))
      )
      eventsQueue = new js.Array()
    }
  }

}
