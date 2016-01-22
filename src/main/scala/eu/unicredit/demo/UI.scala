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
  case class AttachChannel(token: String)

  case class ChannelToken(token: String)

  case object ChannelClosed

  case object AddChannel

}

case class Page() extends VueScalaTagsActor {
  
  def stTemplate = div(
    h1("This is an Akka.js + WebRTC Demo")
  )



  def operational = {
    //val connection1 = context.actorOf(Props(ChannelHandler()))

    val adder = context.actorOf(Props(NewChannelButton()))

    vueBehaviour orElse {
      case PageMsgs.AddChannel =>
        context.actorOf(Props(ChannelHandler()))
    }
  }

  case class NewChannelButton() extends VueScalaTagsActor() {
    def stTemplate = div(
      button(on := {() => {
        context.parent ! PageMsgs.AddChannel
      }})("Add Channel")
    )

    def operational = vueBehaviour
  }


}

case class ChannelHandler() extends VueScalaTagsActor {

  val connection = context.actorOf(Props(new ConnManager()))

  def stTemplate = div(
    hr(),
    p("channel handler")
  )

  def operational = {
    val createButton = context.actorOf(Props(CreateButton()))
    val joinButton = context.actorOf(Props(JoinButton()))

    vueBehaviour orElse {
      case PageMsgs.NewChannel =>
        createButton ! PoisonPill
        joinButton ! PoisonPill
        connection ! ConnMsgs.Open
        context.become(waitingStunOpen)
      case PageMsgs.JoinChannel(token) =>
        createButton ! PoisonPill
        joinButton ! PoisonPill
        connection ! ConnMsgs.Attach(token)
        context.become(waitingStun)
    }
  }

  def waitingStunOpen(): Receive = {
    val waiting = context.actorOf(Props(WaitingToken()))

    vueBehaviour orElse {
      case ConnMsgs.Token(t) =>
        self ! PageMsgs.ChannelToken(t)
      case PageMsgs.ChannelToken(token) =>
        waiting ! PoisonPill
        context.become(attachClient(token))
    }
  }

  def attachClient(token: String): Receive = {
    val tokenHandler = context.actorOf(Props(TokenText(token)))
    val attach = context.actorOf(Props(AttachButton()))

    vueBehaviour orElse {
      case PageMsgs.AttachChannel(token) =>
        tokenHandler ! PoisonPill
        attach ! PoisonPill
        connection ! ConnMsgs.Attach(token)
        context.become(waitingConnection(token))
    }
  }

  def waitingStun(): Receive = {
    val waiting = context.actorOf(Props(WaitingToken()))

    vueBehaviour orElse {
      case ConnMsgs.Token(t) =>
        self ! PageMsgs.ChannelToken(t)
      case PageMsgs.ChannelToken(token) =>
        waiting ! PoisonPill
        context.become(waitingConnection(token))
    }
  }

  def waitingConnection(token: String): Receive = {
    val tokenHandler = context.actorOf(Props(TokenText(token)))

    vueBehaviour orElse {
      case ConnMsgs.Connected =>
        tokenHandler ! PoisonPill
        context.become(connectionDone)
      case PageMsgs.ChannelClosed =>
        tokenHandler ! PoisonPill
        context.become(connectionClosed)
    }
  }

  def connectionDone: Receive = {
    val done = context.actorOf(Props(ConnectedText()))

    vueBehaviour orElse {
      case PageMsgs.ChannelClosed =>
        done ! PoisonPill
        context.become(connectionClosed)
    } 
  }

  def connectionClosed: Receive = {
    val closedText = context.actorOf(Props(ClosedText()))

    vueBehaviour
  }

  case class ConnectedText() extends VueScalaTagsActor() {
    def stTemplate = div(
      p("channel connected!!!!")
    )

    def operational = vueBehaviour
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
      input("v-model".attr := "token")("")
    )

    def operational = vueBehaviour
  }

  case class AttachButton() extends VueScalaTagsActor() {
    def stTemplate = div(
      button(on := {() => {
        context.parent ! PageMsgs.AttachChannel(vue.$get("token").toString)
      }})("ATTACH"),
      input("v-model".attr := "token")("")
    )

    def operational = vueBehaviour
  }
}