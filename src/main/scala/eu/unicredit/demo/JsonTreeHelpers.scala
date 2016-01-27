package eu.unicredit.demo

import scala.scalajs.js
import js.Dynamic.literal
import js.JSON

trait JsonTreeHelpers {

  def genRoot(id: String) = literal(
    root = id
  )

  def addSon(father: String, son: js.Dynamic)(tree: js.Dynamic) = {
    val sonTree = son
  }
}
