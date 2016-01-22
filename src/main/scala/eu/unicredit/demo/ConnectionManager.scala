package eu.unicredit.demo

import scala.scalajs.js
import js.Dynamic.literal
import js.JSON

import akka.actor._

object ConnMsgs {

  case class Node(id: String, channel: ActorRef) { 
    override def equals(x: Any) = {
      x match {
        case Node(xid, _) => xid == id
        case _ => false
      }
    }
  }


non funziona più un cazzo forse conviene ricominciare
ho fottuto la gerarchia dando cose per scontate...

  case object Open
  case class ConnHandler(ref: ActorRef)
  case class AttachAnsware(token: String)
  case class Attach(token: String)

  case class Token(token: String)

  case object Connected

  class TreeMsg
  case class AddParent(node: Node) extends TreeMsg
  case class AddChild(node: Node) extends TreeMsg
  case class Remove(node: Node) extends TreeMsg


  class Status
  case class UpdateRootAdd(id: String, json: String)
  case class UpdateRootRemove(id: String)

  case class UpdateStatus(json: js.Dynamic)
}

class ConnManager() extends Actor {
  import ConnMsgs._

  val id = java.util.UUID.randomUUID.toString

  def receive = operative(None, Seq(), literal(root = id))

  def operative(
      parent: Option[Node],
      sons: Seq[Node],
      status: js.Dynamic): Receive = {
    case Open =>
      val originalSender = sender
      val conn = context.actorOf(Props(WebRTCActor(id, originalSender)))
      conn ! WebRTCMsgs.Create
      originalSender ! ConnHandler(conn)
    case Attach(token) =>
      println("attaching")
      val originalSender = sender
      val conn = context.actorOf(Props(WebRTCActor(id, originalSender)))
      conn ! WebRTCMsgs.Join(token)
    case AddParent(ref) =>
      println("add parent")

      //devo serializzare e deserializzare gli update che vanno sul channel

      ref.channel ! UpdateRootAdd(id, JSON.stringify(status))
      context.become(operative(Some(ref), sons, status))
    case AddChild(ref) =>
      println("add child")
      context.become(operative(parent, sons :+ ref, status))
    case Remove(ref) =>
      (parent) match {
        case Some(p) if (p == ref) =>
          println("TBD -> have to cut this tree")
          val newStatus = status
          sons.foreach(s => s.channel ! UpdateStatus(newStatus))
          context.become(operative(None, sons, newStatus))
        case _ =>
          val newSons = sons.filterNot(_ == ref)
          parent match {
            case Some(p) => 
              p.channel ! UpdateRootRemove(ref.id)
              context.become(operative(parent, newSons, status))
            case _ => //I'm root
              println("TBD -> have to remove "+ref.id+" from tree")
              val newStatus = status 
              sons.foreach(s => s.channel ! UpdateStatus(newStatus))
              context.become(operative(parent, newSons, newStatus))
          }
      }
    case msg @ UpdateStatus(json) =>
      println(id + " NEW STATUS IS --> "+ json)
      sons.foreach(s => s.channel ! msg)
      context.become(operative(parent, sons, json))
    case msg @ UpdateRootAdd(sid, json) =>
      println("ok have to merge this json")
      parent match {
        case Some(p) => p.channel ! msg
        case _ =>
          val newStatus = status
          sons.foreach(s => s.channel ! status)
      }
    case msg @ UpdateRootRemove(sid) =>
      println("ok have to remove this id")
      parent match {
        case Some(p) => p.channel ! msg
        case _ =>
          val newStatus = status
          sons.foreach(s => s.channel ! status)
      }
    case _ =>
  }

}

case class WebRTCActor(parentId: String, tbn: ActorRef) extends Actor {
  import ConnMsgs._

  val conn = 
    context.actorOf(Props(new WebRTCConnection()))

  def receive = waiting

  def waiting: Receive = {
    case WebRTCMsgs.Create =>
      conn ! WebRTCMsgs.Create
      context.become(master)
    case j: WebRTCMsgs.Join =>
      conn ! j
      context.become(slave)
  }

  def master: Receive = {
    case WebRTCMsgs.CreateAnsware(token) =>
      tbn ! ConnMsgs.Token(token)
    case j: WebRTCMsgs.Join =>
      conn ! j
    case WebRTCMsgs.Connected =>
      context.become(
        exchangeUUID((str: String) => AddChild(Node(str, self))))
  }

  def slave: Receive = {
    case WebRTCMsgs.JoinAnsware(token) =>
      tbn ! ConnMsgs.Token(token)
    case WebRTCMsgs.Connected =>
      context.become(
        exchangeUUID((str: String) => AddParent(Node(str, self))))
  }

  def exchangeUUID(msg: (String) => TreeMsg): Receive = {

    import context._
    import scala.concurrent.duration._
    context.system.scheduler.scheduleOnce(200 millis){
      println("sending!!")
      conn ! WebRTCMsgs.MessageToBus(JSON.stringify(literal(id = parentId)))
    }
    ;{
      case WebRTCMsgs.MessageFromBus(json) =>
        println("YEAAAAAA"+json)
        val id = JSON.parse(json).id.toString
        context.parent ! msg(id)
        context.become(standard(id))
      case any =>
        println("unmanaged "+any)
    }
  }  

  def standard(id: String): Receive = {
    tbn ! ConnMsgs.Connected
    ;{
    case m: WebRTCMsgs.MessageToBus =>
      conn ! m
    case m: WebRTCMsgs.MessageFromBus =>
      context.parent ! m
    case WebRTCMsgs.Disconnected =>
      context.parent ! Remove(Node(id, self))
    }
  }

/*
  def receive = {
    case WebRTCMsgs.Connected =>
      context.parent ! ConnMsgs.Add(self)
    case WebRTCMsgs.Disconnected =>
      context.parent ! ConnMsgs.Remove(self)
    case WebRTCMsgs.JoinAnsware(token) =>
      tbn ! ConnMsgs.Token(token)
    case WebRTCMsgs.CreateAnsware(token) =>
      tbn ! ConnMsgs.Token(token)
    case WebRTCMsgs.Create =>
      conn ! WebRTCMsgs.Create
    case j: WebRTCMsgs.Join =>
      conn ! j
    case m: WebRTCMsgs.MessageToBus =>
      conn ! m
    case WebRTCMsgs.MessageFromBus(txt) =>
      println(self.path+" RECEIVED "+txt)
    case any => 
      println(s"unmanaged $any")
  }
*/
}
