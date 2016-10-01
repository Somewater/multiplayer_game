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
export default class EventSocketClient {
  constructor(host, port, verbose = true, pingPong = true) {
    var self = this;
    this.host = host;
    this.port = port;
    this.verbose = verbose;
    this.stateListeners = [];
    this.messageListeners = {};
    this.eventsQueue = [];
    if (pingPong)
      this.on("ping", function(t, p){ self._pingHandler(t, p); });
  }

  start() {
    if (this.status == WebSocket.OPEN) {
      if (this.verbose)
        console.error("Already connected");
      return;
    }
    if (this.status == WebSocket.CONNECTING) {
      if (this.verbose)
        console.error("Already connecting...");
      return;
    }
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
    let self = this;
    this.socket = new WebSocket(`ws://${this.host}:${this.port}/ws`);
    this._dispatchStatus();
    this.socket.onopen = function() {
      self._dispatchStatus();
      self._processOutgointEvents();
    };
    this.socket.onclose = function() {
      self._dispatchStatus();
    };
    this.socket.onmessage = function(e){
      self._onMessage(e);
    };
    this.socket.onerror = function(error) {
      if (self.verbose)
        console.error(error);
    };
    return self;
  };

  reconnect() {
    if (this.status != WebSocket.OPEN && this.status != WebSocket.CONNECTING) {
      this.start();
    }
  }

  get status() {
    if (this.socket)
      return this.socket.readyState;
    else
      return WebSocket.CLOSED;
  }

  _dispatchStatus() {
    var status = this.status;
    if (this.verbose)
      console.log(`WS status: ${status}`)
    for (let cb of this.stateListeners) {
      cb(status);
    }
  }

  addStateListener(listener) {
    this.stateListeners.push(listener);
  }

  on(eventType, listener) {
    if (!this.messageListeners[eventType])
      this.messageListeners[eventType] = [];
    this.messageListeners[eventType].push(listener);
  };

  send(eventType, msg) {
    if (this.status == WebSocket.OPEN) {
      this._send(eventType, msg);
    } else {
      this.eventsQueue.push([eventType, msg]);
    }
  }

  _send(eventType, msg) {
    if (!msg) msg = "";
    this.socket.send(`${eventType}:${msg}`);
  }

  _dispatch(eventType, payload) {
    let listeners = this.messageListeners[eventType];
    if (listeners) {
      for (var cb of listeners) {
        cb(eventType, payload);
      }
    }
  };

  _onMessage(event) {
    let msg = event.data;
    let idx = msg.indexOf(':');
    if (idx == -1 || idx == 0) {
      if (this.verbose)
        console.error(event);
    } else {
      let eventType = msg.substring(0, idx);
      let payload = msg.substring(idx + 1);
      if (this.verbose)
        console.log(`[WS] ${eventType}:${payload}`);
      this._dispatch(eventType, payload);
    }
  }

  _pingHandler(event, ms) {
    var now = new Date().getTime();
    console.log(`Ping duration ${now - parseInt(ms)}`);
    this.send("pong", ms);
  }

  _processOutgointEvents() {
    if (this.eventsQueue.length > 0) {
      for (var ev of this.eventsQueue) {
        this._send(ev[0], ev[1]);
      }
      this.eventsQueue = [];
    }
  }
}
