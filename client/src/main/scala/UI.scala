package eu.unicredit

import akka.actor._

import scala.scalajs.js
import org.scalajs.dom.document.{getElementById => getElem}

import scalatags.JsDom._
import scalatags.JsDom.all._

import TwitterMsgs._

object UIMsgs {}

class UIActor() extends DomActor {
  override val domElement = Some(getElem("root"))

  def template = div()

  val navBar = context.actorOf(Props(new NavBar()))
  val carousel = context.actorOf(Props(new Carousel()))
  val tracker = context.actorOf(Props(new Tracker()))

  val twitterSource = context.actorOf(Props(new TweetSourceActor()))

  override def operative = domManagement orElse {
    case tweet: Tweet =>
      carousel ! tweet
    case track: Track =>
      twitterSource ! track
  }
}

class NavBar() extends DomActor {

  def template = div(cls := "navbar-wrapper")(
    div(cls := "container")(
      div(cls := "navbar navbar-inverse navbar-static-top")(
        div(cls := "container")(
          div(cls := "navbar-header")(
            a(cls := "navbar-brand")(
              "Akka.Js Demo"
            )
          )
        )
      )
    )
  )
}

class Tracker extends DomActor {

  val inputBox = input(`type` := "text",
                       cls := "form-control",
                       attr("placeholder") := "what to track ...").render

  def template = div(cls := "container")(
    div(cls := "col-lg-12")(
      div(cls := "input-group")(
        inputBox,
        span(cls := "input-group-btn")(
          button(cls := "btn btn-default", `type` := "button", onclick := {
            () =>
              val topic = inputBox.value.toString
              context.parent ! Track(topic)
          })(
            "TRACK!"
          )
        )
      )
    )
  )
}

class Carousel extends DomActorWithParams[Tweet] {

  val initValue = Tweet("... waiting first tweet :-) ...", "finger crossed")

  def template(tweet: Tweet) =
    div(id := "myCarousel",
        cls := "carousel slide",
        attr("data-ride") := "carousel")(
      div(cls := "carousel-inner", attr("role") := "listbox")(
        div(cls := "item active")(
          div(cls := "container")(
            div(cls := "carousel-caption")(
              h1(s"${tweet.from}"),
              p(s"${tweet.msg}")
            )
          )
        )
      )
    )

  override def operative = domManagement orElse {
    case t: Tweet =>
      self ! UpdateValue(t)
  }

}
