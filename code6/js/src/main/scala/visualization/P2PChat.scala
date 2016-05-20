package eu.unicredit

import akka.actor._
import AkkaConfig.config

import org.scalajs.dom.document.{getElementById => getElem}

import scalatags.JsDom._
import scalatags.JsDom.all._

object P2PChat {

  implicit lazy val system = ActorSystem("p2pchat", config)

  def run =
    system.actorOf(Props(AddConnection()), "page")

  case class AddConnection() extends DomActor {
    override val domElement = Some(getElem("root"))

    val nameBox =
      input("placeholder".attr := "connection name").render

    def template() = ul(cls := "pure-menu-list")(
        h1("ToDo:"),
        div(cls := "pure-form")(
          nameBox,
          button(
            cls := "pure-button pure-button-primary",
            onclick := {
              () => context.actorOf(Props(ConnectionBox(nameBox.value)))})(
            " Add connection"
          )
        )
      )
  }

  object ConnStatus {
    trait Status

    case object Choose extends Status
    case object BuildOffer extends Status
    case class Offer(token: String) extends Status
    case object BuildAnswer extends Status
    case class Answer(token: String) extends Status    
    case class WaitingAnswer(token: String) extends Status
    case class WaitingConnection(token: String) extends Status
    case object Connected extends Status
  }

  case class ConnectionBox(name: String) extends DomActorWithParams[ConnStatus.Status] {
    import ConnStatus._

    val initValue = Choose

    val conn = context.actorOf(Props(WebRTCConnection(name, self)))

    def template(s: Status) = 
    div(
      h3(s"Channel: $name"),
      s match {
        case Choose =>
          div(
            button(
              cls := "pure-button pure-button-primary",
              onclick := {
                () => self ! UpdateValue(BuildOffer)})(
              "Offer connection"
            ),
            button(
              cls := "pure-button pure-button-primary",
              onclick := {
                () => self ! UpdateValue(BuildAnswer)})(
              "Answer connection"
            )
          )
        case BuildOffer =>
          conn ! WebRTCMsgs.Create

          div(h3("Waiting offer"))
        case Offer(token) =>
          val answerBox =
            input("placeholder".attr := "paste here answer").render    

            div(
              input(value := token/*, "disabled".attr := "true"*/),
              answerBox,
              button(
                cls := "pure-button pure-button-primary",
                onclick := {
                  () => self ! UpdateValue(WaitingConnection(answerBox.value))})(
                "Connect!"
              )
          )
        case BuildAnswer =>
          val offerBox =
            input("placeholder".attr := "paste here offer").render    

            div(
              offerBox,
              button(
                cls := "pure-button pure-button-primary",
                onclick := {
                  () => self ! UpdateValue(WaitingAnswer(offerBox.value))})(
                "Connect!"
              )
          )
        case Answer(token) =>
          div(
            input(value := token/*, "disabled".attr := "true"*/),
            h3("Waiting finalization")
          )
        case WaitingAnswer(token) =>
          conn ! WebRTCMsgs.Join(token)

          div(h3("Waiting answer"))
        case WaitingConnection(token) =>
          conn ! WebRTCMsgs.JoinToken(token)

          div(h3("Waiting connection"))
        case Connected =>
          val treeManager = context.actorOf(Props(TreeManager(name, conn)))

          div(h3("Connected"))
      },
      hr()
    )

    override def operative = domManagement orElse {
      case WebRTCMsgs.OfferToken(token) =>
        self ! UpdateValue(Offer(token))
      case WebRTCMsgs.JoinToken(token) =>
        self ! UpdateValue(Answer(token))
      case WebRTCMsgs.Connected =>
        self ! UpdateValue(Connected)
      case WebRTCMsgs.Disconnected =>
        self ! PoisonPill
    }
  }

}
