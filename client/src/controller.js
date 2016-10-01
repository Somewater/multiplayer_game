export default class Controller {
  constructor(eventClient, view) {
    this.eventClient = eventClient;
    this.view = view;
  }

  start() {
    var self = this;
    this.eventClient.on('position', function(e, p){
      self.onPositionEvent(e, p);
    });
    return this;
  }

  onInited(position, speed, screenSize) {
    this.eventClient.send("position", Point.toPayload(position));
    this.eventClient.send("speed", Point.toPayload(speed));
    this.eventClient.send("screenSize", Point.toPayload(screenSize));
  }

  loop() {

  }

  onPositionEvent(_, payload) {
    let p = Point.fromPayload(payload);
    let cat = this.view.cats[0];
    if (cat) {
      cat.x = p.x;
      cat.y = p.y;
    }
  }
}

class Point {
  constructor(x, y) {
    this.x = x;
    this.y = y;
  }

  static fromPayload(payload) {
    let arr = payload.split(',');
    let x = parseInt(arr[0]);
    let y = parseInt(arr[1]);
    return new Point(x, y);
  }

  static toPayload(pointLike) {
    return `${pointLike.x},${pointLike.y}`;
  }
}