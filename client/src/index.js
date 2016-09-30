var PIXI = require('pixi.js');
var $ = require('jquery');

let evens = [];
var odds = evens.map(v => v + 1);
var nums = evens.map((v, i) => v + i);

class Point {
  constructor(x, y) {
    this.x = x;
    this.y = y;
  }

  move(dx, dy) {
    this.x += dx;
    this.y += dy;

  }

  draw() {
    console.log(`Point(${this.x}, ${this.y})`);
  }
}

class Point3D extends Point {
  constructor(x, y, z) {
    super(x, y);
    this.z = z;
  }

  move(dx, dy, dz) {
    super.move(dx, dy);
    this.z += dz;
  }

  draw() {
    console.log(`Point3D(${this.x}, ${this.y}, ${this.z})`);
  }
}

var p = new Point(10, 20);
p.move(10, 20);
p.draw();

var p3 = new Point3D(0, 3, 4);
p3.move(10, 20, 40)
p3.draw();

const SIZE = 1000;
$(document).ready(function(){
  var renderer = PIXI.autoDetectRenderer(SIZE, SIZE);
  document.body.appendChild(renderer.view);
  var stage = new PIXI.Container();
  renderer.render(stage);

  showImage(renderer, stage);

});

function showImage(renderer, stage) {
  PIXI.loader
    .add([
      "/cat.png"
    ])
    .load(function(){
      var cats = [];
      for (var i = 0; i < 10000; i++) {
        var cat = new PIXI.Sprite(
          PIXI.loader.resources["/cat.png"].texture
        );
        stage.addChild(cat);
        cats.push(cat);

        cat.scale.y = cat.scale.x = Math.random() * 0.5 + 0.5;

        cat.x = Math.random() * SIZE | 0;
        cat.y = Math.random() * SIZE | 0;

        cat.dx = (Math.random() * 10 ) + 1 | 0;
        cat.dy = (Math.random() * 10) + 1 | 0;
      }

      setInterval(function(){
        for (i in cats) {
          var cat = cats[i];
          cat.x += cat.dx;
          if (cat.x < 0 || cat.x > SIZE) cat.dx = -cat.dx;
          cat.y += cat.dy;
          if (cat.y < 0 || cat.y > SIZE) cat.dy = -cat.dy;
        }
        renderer.render(stage);
      }, 20);
    });
}
