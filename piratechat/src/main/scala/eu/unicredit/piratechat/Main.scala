package eu.unicredit.piratechat

import scala.scalajs.js

import akka.actor._

object Main extends js.JSApp {

  val system = ActorSystem("piratechat")

  def main() = {
    println("starting!")

    system.actorOf(Props(PageActor()))
  }

}
