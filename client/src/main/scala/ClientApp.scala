import scala.scalajs.js
import co.technius.scalajs.pixi // The equivalent of PIXI.* classes
import co.technius.scalajs.pixi.Pixi // Contains the PIXI.* vars/functions
import co.technius.scalajs.pixi.loaders.LoaderDSL._ // Type-safe load events

object ClientApp extends js.JSApp {
  def main(): Unit = {
    println("Hello world!")
  }
}