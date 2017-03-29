import scala.collection.mutable
import scala.scalajs.js

/**
  * Can register listeners and dispatch events
  */
class EventDispatcher[EventType, Event]() {
  type Listener = js.Function1[Event, Any]
  private var listeners = mutable.Map.empty[EventType, js.Array[Listener]]

  def on(eventType: EventType, listener: Listener) = {
    listeners.getOrElseUpdate(eventType, new js.Array[Listener]()).push(listener)
  }

  def dispatch(eventType: EventType, event: Event) = {
    listeners.get(eventType) match {
      case Some(evListeners) =>
        for (cb <- evListeners) {
          cb.apply(event)
        }
      case None =>
    }
  }
}
