import proto.game_events

object ImplicitUtils {
  implicit def proto2point(point: Point): game_events.Point = game_events.Point(point.x.toInt, point.y.toInt)
  implicit def point2proto(point: game_events.Point): Point = new Point(point.x, point.y)
}
