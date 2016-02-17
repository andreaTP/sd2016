package eu.unicredit.piratechat

import scala.scalajs.js

import akka.actor._

import org.scalajs.dom

import scalatags.JsDom._
import scalatags.JsDom.all._

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

  case class GitLogin(txt: String)

  case object GistChannel
}

case class PageActor() extends DomActor {

  val id = java.util.UUID.randomUUID.toString

  override val domElement =
    Some(dom.document.getElementById("root"))

  def template() = div(
      h1("Piratechat is borning!")
    )

  var gitLogin = ""

  override def operative = {
    val statusView = context.actorOf(Props(new StatusView(id)), "statusview")

    val connection = context.actorOf(Props(new ConnManager(id, statusView)))

    val setname = context.actorOf(Props(SetName(connection)))

    val gitlogin = context.actorOf(Props(GitUsernameAndPassword()))

    val chat = context.actorOf(Props(new ChatView(id, connection)))

    val adder = context.actorOf(Props(NewChannelButton()))

    domManagement orElse {
      case PageMsgs.AddChannel =>
        context.actorOf(Props(ChannelHandler(statusView, connection)))
      case PageMsgs.GistChannel =>
        context.actorOf(Props(GistChannelHandler(gitLogin, connection)))        
      case m: PageMsgs.ChatMsg =>
        chat ! m
      case gl: PageMsgs.GitLogin =>
        gitLogin = gl.txt
    }
  }

  case class NewChannelButton() extends DomActor {
    def template = div(
      button(onclick := {() => context.parent ! PageMsgs.AddChannel})(
        "Add Manual Connection"),
      button(onclick := {() => context.parent ! PageMsgs.GistChannel})(
        "Add Gist Connection")
    )
  }
}

case class GistChannelHandler(gitLogin: String, connection: ActorRef) extends DomActor {

  val gistActor =
    context.actorOf(Props(GistActor(gitLogin)))

  def template = div(
    button(onclick := {() => self ! ConnMsgs.Open})("CREATE"),
    button(onclick := {() => self ! GistMsgs.Listen})("JOIN")
  )

  override def operative = domManagement orElse {
    case ConnMsgs.Open =>
      connection ! ConnMsgs.Open
      context.become(open)
    case GistMsgs.Listen =>
      gistActor ! GistMsgs.Listen
      context.become(join)
  }

  def join: Receive = domManagement orElse {
    case GistMsgs.AvailableOffer(id, token) =>
      connection ! ConnMsgs.Attach(token)
      context.become(joinOffer(id))
  }

  def joinOffer(id: String): Receive = domManagement orElse {
    case GistMsgs.AvailableAnswer(_, token) =>
      gistActor ! GistMsgs.AnswerForOffer(id, token)
      context.become(waitClose)
  }

  def waitClose: Receive = domManagement orElse {
    case ConnMsgs.Connected =>
      self ! PoisonPill
    case PageMsgs.ChannelClosed =>
      self ! PoisonPill
  }

  def open: Receive = {
    case p: GistMsgs.Publish =>
      val originalSender = sender
      gistActor ! p
      context.become(waitAnswer(originalSender))
  }

  def waitAnswer(ref: ActorRef): Receive = {
    case GistMsgs.AvailableAnswer(id, token) =>
      ref ! WebRTCMsgs.Join(token)
      context.become(waitClose)
  }
}

case class GitUsernameAndPassword() extends DomActor {

  val username = input("").render
  val password = input(tpe := "password")("").render

  def template =
      div(
        label("Git login ->"),
        label("Username"),
        username,
        label("Password"),
        password,
        button(onclick := {() => {
          val gitLogin = username.value.toString + ":" + password.value.toString
          val gitLoginEncoded = js.Dynamic.global.btoa(gitLogin).toString
          context.parent ! PageMsgs.GitLogin(gitLoginEncoded)
          self ! PoisonPill
        }})("SET")
      )

}

case class SetName(connection: ActorRef) extends DomActor {

  val inName = input("v-model".attr := "name")("").render

  def template = div(
    label("Set your name before joining a tree"),
    inName,
    button(onclick := {() => {
      val name = inName.value.trim
      connection ! ConnMsgs.SetName(name)
      self ! PoisonPill
    }})("set")
  )
}

case class ChatView(id: String, connection: ActorRef) extends DomActor {

  val receiver = input("").render
  val message = input("").render

  val inbox = textarea(style := "width: 400px;")("").render

  def template = div(
    label("receiver"),
    receiver,
    label("message"),
    message,
    button(onclick := {() => {
      val receiverText = receiver.value.trim
      val messageText = message.value
      connection ! ConnMsgs.Chat(receiverText, id, messageText)
    }})("send"),
    inbox
  )

  override def operative = domManagement orElse {
      case PageMsgs.ChatMsg(sender, msg) =>
        val txt = "sender: "+sender+" message: "+msg
        val newText =
          if (!js.isUndefined(inbox.innerHTML))
            inbox.innerHTML + "\n" + txt
          else txt

        inbox.innerHTML =  newText
    }
}


case class Node(name: String, descendants: List[Node] = List())

class StatusView(myid: String) extends DomActorWithParams[Node] {
  import svgTags._
  import svgAttrs._
  import paths.high.Tree

  val initValue = Node("-", List())

  def template(n: Node) = {
    try {
    val tree = getTree(n)
    div(
      p("I'm "+myid),
      svg(width := 460, height := 400)(
        g(transform := "translate(80,50)")(
          (branches(tree) ++ nodes(tree)) : _*
        )
      )
    )
    } catch {
      case err: Throwable =>
        err.printStackTrace
        p("error")
    }
  }

  def getTree(treeNodes: Node) = 
    Tree[Node](
      data = treeNodes,
      children = _.descendants,
      width = 300,
      height = 300
    )

  private def move(p: js.Array[Double]) = s"translate(${ p(0) },${ p(1) })"
  private def isLeaf(node: Node) = node.descendants.length == 0  

  def branches(tree: Tree[Node]) = tree.curves map { curve =>
    path(d := curve.connector.path.print,
      stroke := "grey", 
      fill := "none"
    )
  }

  def nodes(tree: Tree[Node]) = tree.nodes map { node =>
    g(transform := move(node.point),
      circle(r := 5, cx := 0, cy := 0),
      text(
        transform := (if (isLeaf(node.item)) "translate(10,0)" else "translate(-10,0)"),
        textAnchor := (if (isLeaf(node.item)) "start" else "end"),
        node.item.name
      )
    )
  }

  def fromJsonToNode(tree: js.Dynamic, id: String): Node = {
    val name = tree.selectDynamic(id).name
    val symName = 
      if (js.isUndefined(name)) {
        if (myid == id)
          "ME: "+id.toString
        else 
          id
      } else if (myid == id)
        "ME: "+name.toString
      else
        name.toString

    Node(symName, tree.selectDynamic(id).sons.asInstanceOf[js.Array[String]].map(sid =>
      fromJsonToNode(tree, sid.toString)
    ).toList)
  }  

  override def operative = domManagement orElse {
    case PageMsgs.NewStatus(tree) =>
      self ! UpdateValue(fromJsonToNode(tree, tree.root.toString))
  }

}

case class ChannelHandler(statusView: ActorRef, connection: ActorRef) extends DomActor {

  def template = div(
    hr(),
    button(onclick := {() => self ! PoisonPill})("close box")
  )

  override def operative = {
    val createButton = context.actorOf(Props(CreateButton()))
    val joinButton = context.actorOf(Props(JoinButton()))

    domManagement orElse {
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

    domManagement orElse {

      case GistMsgs.Publish(t) =>
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

    domManagement orElse {
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

    domManagement orElse {
      case GistMsgs.AvailableAnswer(_, t) =>
        val originalSender = sender
        self ! PageMsgs.ChannelToken(t, originalSender)
      case PageMsgs.ChannelToken(token, _) =>
        waiting ! PoisonPill
        context.become(waitingConnection(token))
    }
  }

  def waitingConnection(token: String): Receive = {
    val tokenHandler = context.actorOf(Props(TokenText(token)))

    domManagement orElse {
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

    domManagement orElse {
      case PageMsgs.ChannelClosed =>
        done ! PoisonPill
        context.become(connectionClosed)
    } 
  }

  def connectionClosed: Receive = {
    val closedText = context.actorOf(Props(ClosedText()))

    domManagement
  }

  case class ConnectedText() extends DomActor {
    def template = div(
      p("channel connected!!!!")
    )
  }

  case class ClosedText() extends DomActor {
    def template = div(
      p("channel closed"),
      button(onclick := {() => context.parent ! PoisonPill})("remove")
    )
  }

  case class TokenText(token: String) extends DomActor {
    def template = div(
      p("token is:"),
      input(value := token)
    )
  }

  case class WaitingToken() extends DomActor {
    def template = div(
      p("wait please")
    )
  }

  case class CreateButton() extends DomActor {
    def template = div(
      button(onclick := {() => {
        context.parent ! PageMsgs.NewChannel
      }})("CREATE")
    )
  }

  case class JoinButton() extends DomActor {

    val token = input("").render

    def template = div(
      button(onclick := {() => {
        println("input is "+token.value)
        context.parent ! PageMsgs.JoinChannel(token.value)
      }})("JOIN"),
      token
    )
  }

  case class AttachButton() extends DomActor {

    val token = input("").render

    def template = div(
      button(onclick := {() => {
        context.parent ! PageMsgs.AttachChannel(token.value)
      }})("ATTACH"),
      token
    )
  }
}
