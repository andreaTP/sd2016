package eu.unicredit

import scala.scalajs.js
import js.Dynamic.{global => g}

import akka.actor._

object Main extends js.JSApp {
  def main() = {

    val system = ActorSystem("node-server")

    val wsServerActor =
      system.actorOf(Props(new WSServerActor(9002)), "wsServer")

  }

  val credentials = loadCredentials()

  def loadCredentials() = {
    val fs = g.require("fs")
    /*credentials format:
     *{
     * "consumer_key": "",
     *  "consumer_secret": "",
     *  "token": "",
     *  "token_secret": ""
     *}
     */
    js.JSON.parse(fs.readFileSync(".credentials", "utf8").toString)
  }
}
