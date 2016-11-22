package eu.unicredit

import scala.scalajs.js
import js.Dynamic.{global => g}

import akka.actor._

object DemoTwitterActor extends js.JSApp {
  def main() = {

    val system = ActorSystem("node-twitter-demo")

    system.actorOf(Props(new Actor {
      import TwitterMsgs._
      val twitterActor = context.actorOf(Props(new TwitterActor()))

      twitterActor ! Track("Pizza")

      def receive = {
        case msg =>
          println(s"Received: $msg")
      }
    }))
  }
}
