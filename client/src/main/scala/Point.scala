import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@JSExport
class Point(var x: Double, var y: Double) {
  def copyFrom(point: Point): Unit = {
    this.x = point.x
    this.y = point.y
  }
}

object Point {
  def fromPayload(payload: String, to: Point = null): Point = {
    val arr = payload.split(",")
    val x = arr(0).toInt
    val y = arr(1).toInt
    if (to == null) {
      new Point(x, y)
    } else {
      to.x = x
      to.y = y
      to
    }
  }

  def toPayload(v: Point) = {
    //val v = js.use(pointLike).as[PointLike]
    s"${v.x.toInt},${v.y.toInt}"
  }
}