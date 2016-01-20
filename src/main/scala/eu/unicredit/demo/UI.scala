package eu.unicredit.demo

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js
import js.annotation._

import akka.actor._

import scalatags.Text.all._

object PageMsgs {

  case object NewChannel
  case class JoinChannel(token: String)

  case class ChannelToken(token: String)

  case object ChannelClosed

}

case class Page() extends VueScalaTagsActor {
  
  def stTemplate = div(
    h1("This is an Akka.js + WebRTC Demo")
  )

  def operational = {
    val connection1 = context.actorOf(Props(ChannelHandler()))

    vueBehaviour
  }

}

case class ChannelHandler() extends VueScalaTagsActor {

  def stTemplate = div(
    h4("This is a channel handler")
  )

  def operational = {
    val createButton = context.actorOf(Props(CreateButton()))
    val joinButton = context.actorOf(Props(JoinButton()))

    vueBehaviour orElse {
      case PageMsgs.NewChannel =>
        createButton ! PoisonPill
        joinButton ! PoisonPill
        context.become(waitingStun)
      case PageMsgs.JoinChannel(token) =>
        createButton ! PoisonPill
        joinButton ! PoisonPill
        context.become(waitingStun)
    }
  }

  def waitingStun(): Receive = {
    val waiting = context.actorOf(Props(WaitingToken()))

    vueBehaviour orElse {
      case PageMsgs.ChannelToken(token) =>
        waiting ! PoisonPill
        context.become(connectionDone(token))
    }
  }

  def connectionDone(token: String): Receive = {
    val tokenHandler = context.actorOf(Props(TokenText(token)))

    vueBehaviour orElse {
      case PageMsgs.ChannelClosed =>
        tokenHandler ! PoisonPill
        context.become(connectionClosed)
    }
  }

  def connectionClosed: Receive = {
    val closedText = context.actorOf(Props(ClosedText()))

    vueBehaviour
  }

  case class ClosedText() extends VueScalaTagsActor() {
    def stTemplate = div(
      p("channel closed"),
      button(on := {() => context.parent ! PoisonPill})("remove")
    )

    def operational = vueBehaviour
  }

  case class TokenText(token: String) extends VueScalaTagsActor() {
    def stTemplate = div(
      p("token is:"),
      input(value := token)
    )

    def operational = vueBehaviour
  }

  case class WaitingToken() extends VueScalaTagsActor() {
    def stTemplate = div(
      p("wait please")
    )

    def operational = vueBehaviour
  }

  case class CreateButton() extends VueScalaTagsActor() {
    def stTemplate = div(
      button(on := {() => {
        context.parent ! PageMsgs.NewChannel
      }})("CREATE")
    )

    def operational = vueBehaviour
  }

  case class JoinButton() extends VueScalaTagsActor() {
    def stTemplate = div(
      button(on := {() => {
        context.parent ! PageMsgs.JoinChannel(vue.$get("token").toString)
      }})("JOIN"),
      input("{{ token }}")
    )

    def operational = vueBehaviour
  }
}