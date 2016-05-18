package eu.unicredit

import akka.actor._
import AkkaConfig.config

import org.scalajs.dom.document.{getElementById => getElem}

import scalatags.JsDom._
import scalatags.JsDom.all._

import org.scalajs.dom.raw._

object ChatUI {

  implicit lazy val system = ActorSystem("chat", config)

  def start =
    system.actorOf(Props(ChatUI()), "page")

  case class ChatUI() extends DomActor {
    override val domElement = Some(getElem("root"))

    val urlBox = input("placeholder".attr := "enter url here").render

    def template() = div(
      h1("Chat it!"),
      urlBox,
      button(onclick := {
        () => {
          context.actorOf(Props(ChatBox(urlBox.value)))
        }
      })("Connect")
    )
  }

  case class ChatBox(wsUrl: String) extends DomActorWithParams[List[String]] {

    case class NewMsg(txt: String)

    val ws = new WebSocket(s"ws://$wsUrl")
    ws.onmessage = { (event: MessageEvent) => self ! NewMsg(event.data.toString)}

    val initValue = List()

    val msgBox = input("placeholder".attr := "enter message").render

    def template(txt: List[String]) = div(
      p(s"url -> $wsUrl"),
      button(onclick := {() => self ! PoisonPill})("Close"),
      msgBox,
      button(onclick := {() => ws.send(msgBox.value)})("Send"),
      ul(for (t <- txt) yield li(t))
    )

    override def operative = withText(initValue)

    def withText(last: List[String]): Receive = domManagement orElse {
      case NewMsg(txt) =>
        val newTxt = (last :+ txt).takeRight(5)
        self ! UpdateValue(newTxt)
        context.become(withText(newTxt))
    }
  }

}