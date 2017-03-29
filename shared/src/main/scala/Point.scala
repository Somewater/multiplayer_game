import scala.scalajs.js.annotation.JSExport

@JSExport
class Point(var x: Double, var y: Double) {
  def copyFrom(point: Point): Unit = {
    this.x = point.x
    this.y = point.y
  }

  def toPayload = {
    s"${x.toInt},${y.toInt}"
  }
}
