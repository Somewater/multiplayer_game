import scala.scalajs.js

class View() {
  val cats = js.Array[Cat]()
  val catsByIndex = js.Dictionary[Cat]()

  def addCat(cat: Cat) = {
    cats.push(cat)
    catsByIndex.put(cat.index, cat)
  }

  def removeCat(cat: Cat) = {
    val idx = cats.indexOf(cat)
    if (idx >= 0)
      cats.remove(idx)
    catsByIndex.remove(cat.index)
    if (cat.view != null)
      cat.view.parent.removeChild(cat.view)
  }
}
