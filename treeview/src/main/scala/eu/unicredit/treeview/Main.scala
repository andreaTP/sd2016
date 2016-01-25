package eu.unicredit.treeview

import scala.scalajs.js

import scala.scalajs.js.Dynamic.global

object Main extends js.JSApp {

  def main() = {
    println("ciao!!!")

    global.document.getElementById("root").innerHTML =  "<h1>CIAO</h1>"
  }
  
}