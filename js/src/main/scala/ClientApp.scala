import co.technius.scalajs.pixi
import co.technius.scalajs.pixi.Pixi
import co.technius.scalajs.pixi.loaders._

import scala.scalajs.js
import org.scalajs.dom
import org.scalajs.jquery.jQuery

import scala.scalajs.js.annotation.JSExport // Type-safe load events

object ClientApp extends js.JSApp {

  val eventClient = new EventSocketClient("localhost", 61618) with Reconnect
  val view = new View()
  val controller = new Controller(eventClient, view)
  val SIZE = 300

  def main(): Unit = {
    eventClient.startWithReconnect()
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
    controller.onInited(new Point(SIZE, SIZE))
    dom.window.setInterval(onTick(renderer, stage) _, 20)
  }

  def onTick(renderer: pixi.SystemRenderer, stage: pixi.Container)() = {
    for (cat <- view.cats) {
      if (cat.view == null) {
        cat.view = new pixi.Sprite(Pixi.loader.resources("/cat.png").texture.get)
        stage.addChild(cat.view)
      }
      cat.view.scale.set(cat.size.x * 0.1, cat.size.y * 0.1)
      cat.view.x = cat.position.x
      cat.view.y = cat.position.y
    }

    renderer.render(stage)
  }
}

