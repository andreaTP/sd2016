package eu.unicredit.demo

import scala.util.Try
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

  case class ChannelToken(token: String, ref: ActorRef)

  case object ChannelClosed

  case object AddChannel

  case class NewStatus(tree: js.Dynamic)

  case class ChatMsg(sender: String, text: String)

  case object Closed
}

case class Page() extends VueScalaTagsActor {
  
  def stTemplate = div(
    p("This is an Akka.js + WebRTC Demo")
  )

  val id = java.util.UUID.randomUUID.toString

  def operational = {
    //val connection1 = context.actorOf(Props(ChannelHandler()))

    val statusView = context.actorOf(Props(new StatusView(id)))

    val connection = context.actorOf(Props(new ConnManager(id, statusView)))

    val setname = context.actorOf(Props(SetName(connection)))

    val chat = context.actorOf(Props(new ChatView(id, connection)))

    val adder = context.actorOf(Props(NewChannelButton()))

    vueBehaviour orElse {
      case PageMsgs.AddChannel =>
        context.actorOf(Props(ChannelHandler(statusView, connection)))
      case m: PageMsgs.ChatMsg =>
        chat ! m
    }
  }

  case class NewChannelButton() extends VueScalaTagsActor() {
    def stTemplate = div(
      button(on := {() => {
        context.parent ! PageMsgs.AddChannel
      }})("Add Connection")
    )

    def operational = vueBehaviour
  }
}

case class SetName(connection: ActorRef) extends VueScalaTagsActor {

  def stTemplate = div(
    label("Set your name before joining a tree"),
    input("v-model".attr := "name")(""),
    button(on := {() => {
      val name = vue.$get("name").toString.trim
      connection ! ConnMsgs.SetName(name)
      self ! PoisonPill
    }})("set")
  )

  def operational = vueBehaviour
}

case class ChatView(id: String, connection: ActorRef) extends VueScalaTagsActor {

  def stTemplate = div(
    label("receiver"),
    input("v-model".attr := "receiver")(""),
    label("message"),
    input("v-model".attr := "message")(""),
    button(on := {() => {
      val receiver = vue.$get("receiver").toString.trim
      val message = vue.$get("message").toString
      connection ! ConnMsgs.Chat(receiver,id,message)
    }})("send"),
    textarea("v-model".attr := "inbox",
      style := "width: 400px;")("")
  )

    def operational = vueBehaviour orElse {
      case PageMsgs.ChatMsg(sender, msg) =>
        val txt = "sender: "+sender+" message: "+msg
        val newText =
          if (!js.isUndefined(vue.$get("inbox")))
            vue.$get("inbox").toString + "\n" + txt
          else txt

        vue.$set("inbox", newText)
    }
}

case class ChannelHandler(statusView: ActorRef, connection: ActorRef) extends VueScalaTagsActor {

  def stTemplate = div(
    hr(),
    button(on := {() => self ! PoisonPill})("close box")
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
        val originalSender = sender
        self ! PageMsgs.ChannelToken(t, originalSender)
      case PageMsgs.ChannelToken(token, ref) =>
        waiting ! PoisonPill
        context.become(attachClient(token, ref))
      }
  }

  def attachClient(token: String, conn: ActorRef): Receive = {
    val tokenHandler = context.actorOf(Props(TokenText(token)))
    val attach = context.actorOf(Props(AttachButton()))

    vueBehaviour orElse {
      case PageMsgs.AttachChannel(token) =>
        tokenHandler ! PoisonPill
        attach ! PoisonPill
        conn ! WebRTCMsgs.Join(token)
        //connection ! ConnMsgs.Attach(token)
        context.become(waitingConnection(token))
    }
  }

  def waitingStun(): Receive = {
    val waiting = context.actorOf(Props(WaitingToken()))

    vueBehaviour orElse {
      case ConnMsgs.Token(t) =>
        val originalSender = sender
        self ! PageMsgs.ChannelToken(t, originalSender)
      case PageMsgs.ChannelToken(token, _) =>
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

case class Node(name: String, descendants: List[Node] = List())

class StatusView(myid: String) extends VueScalaTagsActor {
  
  def stTemplate = div()

  def fromJsonToNode(tree: js.Dynamic, id: String): Node = {
    val name = tree.selectDynamic(id).name
    val symName = 
      if (js.isUndefined(name))
        id
      else if (myid == id)
        "ME: "+name.toString        
      else
        name.toString

    Node(symName, tree.selectDynamic(id).sons.asInstanceOf[js.Array[String]].map(sid =>
      fromJsonToNode(tree, sid.toString)
    ).toList)
  }  

  def operational = vueBehaviour orElse {
    case PageMsgs.NewStatus(tree) =>
      context.become(
        treeview(myid, fromJsonToNode(tree, tree.root.toString)))
  }

  def treeview(name: String, node: Node): Receive = {
    val tvactor = context.actorOf(Props(new TreeView(name, node)))

    vueBehaviour orElse {
      case PageMsgs.NewStatus(tree) =>
        tvactor ! PoisonPill
        context.become(
          waitingClose(name, fromJsonToNode(tree, tree.root.toString)))
    }
  }

  def waitingClose(name: String, node: Node): Receive =
    vueBehaviour orElse {
      case PageMsgs.Closed =>
        context.become(
          treeview(name, node))
    }
}

class TreeView(name: String, treeNodes: Node) extends VueScalaTagsActor {
  import scalatags.Text._
  import svgTags._
  import svgAttrs._
  import paths.high.Tree

  val tree = Tree[Node](
    data = treeNodes,
    children = _.descendants,
    width = 300,
    height = 300
  )

  private def move(p: js.Array[Double]) = s"translate(${ p(0) },${ p(1) })"
  private def isLeaf(node: Node) = node.descendants.length == 0  

  val branches = tree.curves map { curve =>
    path(d := curve.connector.path.print,
      stroke := "grey", 
      fill := "none"
    )
  }

  val nodes = tree.nodes map { node =>
    g(transform := move(node.point),
      circle(r := 5, cx := 0, cy := 0),
      text(
        transform := (if (isLeaf(node.item)) "translate(10,0)" else "translate(-10,0)"),
        textAnchor := (if (isLeaf(node.item)) "start" else "end"),
        node.item.name
      )
    )
  }

  def stTemplate = div(
    p("I'm "+name),
    svg(width := 460, height := 400)(
      g(transform := "translate(80,50)")(
        (branches ++ nodes) : _*
      )
    )
  )

  def operational = vueBehaviour

  override def postStop() = {
    Try{ super.postStop() }
    context.parent ! PageMsgs.Closed
  }
}
