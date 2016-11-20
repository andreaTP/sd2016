package eu.unicredit

import scala.scalajs.js

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._

import scala.util.Try
import scala.util.control.NonFatal
import scala.concurrent.duration._

import org.scalajs.dom

object TwitterMsgs {
  case class Tweet(from: String, msg: String)
  case class Track(topic: String)
}

class TweetSourceActor() extends Actor {
  import TwitterMsgs._
  import context.dispatcher

  implicit val materializer = ActorMaterializer()

  val wsQueue =
    Source
      .queue[String](10, OverflowStrategy.dropTail)
      .map(elem => {
        try {
          val tweet = js.JSON.parse(elem)
          Some(Tweet(tweet.user.name.toString, tweet.text.toString))
        } catch {
          case NonFatal(e) => None
        }
      })
      .filter(_.isDefined)
      .map(_.get)
      .throttle(1, 750 millis, 1, ThrottleMode.shaping)
      .to(Sink.actorRef(self, PoisonPill))
      .run()

  val ws = new dom.WebSocket(s"ws://localhost:9002")

  ws.onmessage = { (event: dom.MessageEvent) =>
    wsQueue.offer(event.data.toString)
  }

  case object Ready

  ws.onopen = { (event: dom.Event) =>
    self ! Ready
  }

  def receive = {
    case Ready =>
      context.become(operative)
  }

  def operative: Receive = {
    case tweet: Tweet =>
      context.parent ! tweet
    case track: Track =>
      ws.send(track.topic)
  }

  override def postStop() =
    ws.close()

}
