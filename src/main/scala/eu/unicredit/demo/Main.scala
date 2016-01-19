package eu.unicredit.demo

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js
import js.annotation._

import akka.actor._

@JSExport("Main")
object Main extends js.JSApp {

  //devo creare un attore che gestisca sta roba...

  val system = ActorSystem("akkajs")

  val act =
    system.actorOf(Props(new WebRTCActor()), "one")

  val act2 =
    system.actorOf(Props(new WebRTCActor()), "two")

  def main() = {
    println("starting!")

  }

  case class WebRTCActor() extends Actor {

    val conn = 
      context.actorOf(Props(new WebRTCConnection()))

    def receive = {
      case WebRTCMsgs.Connected => println("GREAT!!!")
      case WebRTCMsgs.Disconnected => println("disconnected")
      case WebRTCMsgs.JoinAnsware(token) => println(s"${self.path} joined token is \n$token")
      case WebRTCMsgs.CreateAnsware(token) => println(s"${self.path} create token is \n$token")
      case WebRTCMsgs.Create =>
        conn ! WebRTCMsgs.Create
      case j: WebRTCMsgs.Join =>
        conn ! j
      case m: WebRTCMsgs.MessageToBus =>
        conn ! m
      case WebRTCMsgs.MessageFromBus(txt) =>
        println(self.path+" RECEIVED "+txt)
      case any => 
        println(s"unmanaged $any")
    }
  }

  @JSExport
  def create() = {
    act ! WebRTCMsgs.Create
  }

  @JSExport
  def create2() = {
    act2 ! WebRTCMsgs.Create
  }

  @JSExport
  def attach(txt: String) = {
    act ! WebRTCMsgs.Join(txt)
  }

  @JSExport
  def attach2(txt: String) = {
    act2 ! WebRTCMsgs.Join(txt)
  }

  @JSExport
  def join(txt: String) = {
    act ! WebRTCMsgs.Join(txt)
  }

  @JSExport
  def ping() = {
    act ! WebRTCMsgs.MessageToBus("ping")
  }

  @JSExport
  def ping2() = {
    act2 ! WebRTCMsgs.MessageToBus("ping2")
  }

}