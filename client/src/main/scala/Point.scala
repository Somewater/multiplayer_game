import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@JSExport
class Point(val x: Double, val y: Double) {

}

object Point {
  def fromPayload(payload: String): Point = {
    val arr = payload.split(",")
    val x = arr(0).toInt
    val y = arr(1).toInt
    new Point(x, y)
  }

  def toPayload(v: Point) = {
    //val v = js.use(pointLike).as[PointLike]
    s"${v.x.toInt},${v.y.toInt}"
  }
}