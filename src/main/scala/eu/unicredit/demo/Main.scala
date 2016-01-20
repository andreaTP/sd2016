package eu.unicredit.demo

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js
import js.annotation._

import akka.actor._

@JSExport("Main")
object Main extends js.JSApp {

  val system = ActorSystem("akkajs")

  val act =
    system.actorOf(Props(new WebRTCActor(null)), "one")

  val act2 =
    system.actorOf(Props(new WebRTCActor(null)), "two")

  def main() = {
    println("starting!")

    VueActor.setSystem(system)

    system.scheduler.scheduleOnce(0 millis)(
      VueActor.insert(() => new Page(), "page"))

/*
    case class Page() extends VueScalaTagsActor {
      import scalatags.Text.all._

      def stTemplate = h1("Ciao")

      def operational = vueBehaviour
    }
*/
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