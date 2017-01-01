import co.technius.scalajs.pixi
import co.technius.scalajs.pixi.Pixi
import co.technius.scalajs.pixi.loaders._

import scala.scalajs.js
import org.scalajs.dom
import org.scalajs.jquery.jQuery

import scala.scalajs.js.annotation.JSExport // Type-safe load events

object ClientApp extends js.JSApp {

  val eventClient = new EventSocketClient("localhost", 61618)
  val view = new View()
  val controller = new Controller(eventClient, view)
  val SIZE = 300

  def main(): Unit = {
    eventClient.start()
    controller.start()
    jQuery(dom.document).ready(onReady _)
  }

  def onReady = {
    val renderer = Pixi.autoDetectRenderer(SIZE, SIZE)
    dom.document.body.appendChild(renderer.view)
    val stage = new pixi.Container()
    renderer.render(stage)
    Pixi.loader.add("/cat.png").load(onImgLoaded(renderer, stage) _)
  }

  def onImgLoaded(renderer: pixi.SystemRenderer, stage: pixi.Container)
                 (loader: Loader, dict: ResourceDictionary): Unit = {
    val cats = view.cats
    for (i <- (0 to 1)) {
      val catView = new pixi.Sprite(dict("/cat.png").texture.get)
      val cat = new Cat(catView)
      stage.addChild(catView)
      cats.push(cat)

      val scale = Math.random() * 0.5 + 0.5
      catView.scale.set(scale, scale)

      catView.x = Math.random() * SIZE
      catView.y = Math.random() * SIZE

      cat.dx = (Math.random() * 10) + 1
      cat.dy = (Math.random() * 10) + 1
    }



    var c = cats(0)
    controller.onInited(
      new Point(c.view.x, c.view.y),
      new Point(c.dx, c.dy),
      new Point(SIZE, SIZE))

    dom.window.setInterval(onTick(renderer, stage) _, 20)
  }

  def onTick(renderer: pixi.SystemRenderer, stage: pixi.Container)() = {
    renderer.render(stage)
  }
}

class Cat(val view: pixi.Sprite, var dx: Double = 0, var dy: Double = 0)

