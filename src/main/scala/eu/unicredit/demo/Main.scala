package eu.unicredit.demo

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js
import js.annotation._

import akka.actor._

@JSExport("Main")
object Main extends js.JSApp {

  val system = ActorSystem("akkajs")

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

}