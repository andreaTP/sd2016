package eu.unicredit.gists

import scala.concurrent.duration._
import akka.actor._

import scala.scalajs.js
import js.annotation.JSExport

import js.Dynamic.literal
import js.JSON

import org.scalajs.dom.ext.Ajax
import scala.concurrent.ExecutionContext.Implicits.global

@JSExport("Main")
object Main extends js.JSApp {
  import GistMsgs._

  val system = ActorSystem("gists")

  val manager = system.actorOf(Props(HandshakeManager()))

  def main() = {
    println("starting...")
  }

  @JSExport
  var username = ""

  @JSExport
  var password = ""

  @JSExport
  def publish() =
    manager ! Publish("")

  @JSExport
  def listen() =
    manager ! Listen


  
  case class HandshakeManager() extends Actor {

    def newUUID() =
      java.util.UUID.randomUUID().toString

    def newGistActor() =
      context.actorOf(Props(GistActor(
        js.Dynamic.global.btoa(username + ":" + password).toString
        )))

    def receive = {
      case msg: Publish =>
        val uuid = newUUID()
        println("publishing offer "+uuid)
        newGistActor ! Publish("this is an offer "+uuid)
      case Listen =>
        newGistActor ! Listen

      //Publisher answer
      case AvailableAnswer(id, token) =>
        val originalSender = sender
        println("I can close connection with token "+token)

        originalSender ! PoisonPill
      //Listener request
      case AvailableOffer(id, token) =>
        val originalSender = sender
        println("seen offer with token "+token)
        val uuid = newUUID()
        println("writing answer "+uuid)
        originalSender ! AnswerForOffer(id, "this is an answer "+uuid)

        context.system.scheduler.scheduleOnce(10 seconds)(originalSender ! PoisonPill)        
    }
  }

}