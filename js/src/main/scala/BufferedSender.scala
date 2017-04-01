import scala.scalajs.js

/**
  * Send if available or send later
  */
class BufferedSender[Payload](initialOpen: Boolean = false)(realSend: Payload => Any) {
  private var isOpen: Boolean = initialOpen
  private var queue = new js.Array[Payload]

  def send(payload: Payload): Boolean = {
    if (isOpen) {
      realSend(payload)
      true
    } else {
      queue.push(payload)
      false
    }
  }

  def open() = {
    if (!isOpen) {
      for (payload <- queue) {
        realSend(payload)
      }
      queue = new js.Array[Payload]
      isOpen = true
    }
  }

  def close() = {
    isOpen = false
  }

  def toggle() = {
    if (isOpen) close()
    else open()
  }
}
