package eu.unicredit.backbone

import scala.scalajs.js
import js.Dynamic.literal
import org.scalajs.dom
import java.util.UUID

import akka.actor._

import js.Dynamic.{ global => g, newInstance => jsnew }

object Main extends js.JSApp {

  def main() = {
    println("Start here...")

    val system = ActorSystem("domsystem")

    system.actorOf(Props(HelloActor()))

  }
}

case class HelloActor() extends DomActor {
  import scalatags.JsDom._
  import scalatags.JsDom.all._

  override val domElement =
    Some(dom.document.getElementById("container"))

  def template() = div(
      h1("Hello world!")
    )

  override def operative = {

    context.actorOf(Props(LiActor()))
    context.actorOf(Props(ButtonActor()))
    
    domManagement
  }
}

case class ButtonActor() extends DomActor {
  import scalatags.JsDom._
  import scalatags.JsDom.all._

  def template() = div(
    button(
      onclick := {() => println("ciao!!!")}
      )(
      "di ciao")
    )

}

case class LiActor() extends DomActorWithParams[Int] {
  import scalatags.JsDom._
  import scalatags.JsDom.all._

  val initValue: Int = 0

  def template(value: Int) = 
    ul(
      for (i <- 0 until value) yield
        li(i.toString)
    )

  case object More
  import scala.concurrent.duration._
  import context._
  val c = context.system.scheduler.schedule(1 seconds, 1 second)({
    self ! More
  })

  override def operative = increment(0)

  def increment(last: Int): Receive = 
  domManagement orElse {
    case More =>
      if (last < 5) {
        self ! UpdateValue(last)
        context.become(increment(last + 1))
      } else {
        c.cancel
        self ! PoisonPill
      }
  }
}
