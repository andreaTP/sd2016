package eu.unicredit

import scala.scalajs.js
import js.Dynamic.{global => g}

import akka.actor._

class WSTwitterChannelActor(connection: js.Dynamic) extends Actor {
  import TwitterMsgs._

  val twitterActor = context.actorOf(Props(new TwitterActor()))

  connection.on("message", (message: js.Dynamic) => {
    twitterActor ! Track(message.utf8Data.toString)
  })

  connection.on("close", (reasonCode: js.Dynamic, description: js.Dynamic) => {
    self ! PoisonPill
  })

  def receive = {
    case tweet: String =>
      connection.send(tweet)
  }
}
