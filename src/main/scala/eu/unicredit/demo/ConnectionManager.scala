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

  case object Open
  case class Attach(token: String)

  case class Token(token: String)

  case object Connected

  class TreeMsg
  case class AddParent(node: Node) extends TreeMsg
  case class AddChild(node: Node) extends TreeMsg
  case class Remove(node: Node) extends TreeMsg


  class Status
  case class UpdateRootAdd(id: String, json: String) extends 
    WebRTCMsgs.MessageToBus(JSON.stringify(literal(updateRootAdd = id, content = json)))
  case class UpdateRootRemove(id: String) extends
    WebRTCMsgs.MessageToBus(JSON.stringify(literal(updateRootRemove = id)))

  case class UpdateStatus(json: js.Dynamic) extends
    WebRTCMsgs.MessageToBus(JSON.stringify(literal(updateStatus = json)))
}

class ConnManager(statusView: ActorRef) extends Actor with JsonTreeHelpers {
  import ConnMsgs._

  val id = java.util.UUID.randomUUID.toString

  println("I am "+id)

  def receive = {
    val status = emptyRoot(id)
    statusView ! PageMsgs.NewStatus(status)

    operative(None, Seq(), status)
  }

  def operative(
      parent: Option[Node],
      sons: Seq[Node],
      status: js.Dynamic): Receive = {
    case Open =>
      val originalSender = sender
      val conn = context.actorOf(Props(WebRTCActor(id, originalSender)))
      conn ! WebRTCMsgs.Create
    case Attach(token) =>
      val originalSender = sender
      val conn = context.actorOf(Props(WebRTCActor(id, originalSender)))
      conn ! WebRTCMsgs.Join(token)
    case AddParent(ref) =>
      ref.channel ! UpdateRootAdd(ref.id, JSON.stringify(status))
      context.become(operative(Some(ref), sons, status))
    case AddChild(ref) =>
      context.become(operative(parent, sons :+ ref, status))
    case Remove(ref) =>
      (parent) match {
        case Some(p) if (p.id != ref.id) =>
          p.channel ! UpdateRootRemove(ref.id)
        case _ =>
          self ! UpdateRootRemove(ref.id)
      }
    case msg @ UpdateStatus(json) =>
      statusView ! PageMsgs.NewStatus(json)
      sons.foreach(s => s.channel ! msg)
      context.become(operative(parent, sons, json))
    case msg @ UpdateRootAdd(fatherId, json) =>
      parent match {
        case Some(p) => p.channel ! msg
        case _ =>

          merge(fatherId)(status, JSON.parse(json))

          statusView ! PageMsgs.NewStatus(status)
          sons.foreach(s => s.channel ! UpdateStatus(status))
      }
    case msg @ UpdateRootRemove(sid) =>
      parent match {
        case Some(p) =>
          if (p.id == sid) {

            keep(id)(status)

            statusView ! PageMsgs.NewStatus(status)
            sons.foreach(s => s.channel ! UpdateStatus(status))
            context.become(operative(None, sons, status))
          } else {
            p.channel ! msg
          }
        case _ =>
          val newSons = sons.filterNot(_ == sid)

          remove(sid)(status)

          statusView ! PageMsgs.NewStatus(status)
          newSons.foreach(s => s.channel ! UpdateStatus(status))
          context.become(operative(parent, newSons, status))
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

    def sendHandShake() = {
      import context._
      import scala.concurrent.duration._
      context.system.scheduler.scheduleOnce(200 millis){
        println("sending!!")
        conn ! new WebRTCMsgs.MessageToBus(JSON.stringify(literal(id = parentId)))
      }
    }
    sendHandShake()
    ;{
      case fb: WebRTCMsgs.MessageFromBus =>
        val json = fb.txt
        
        if (js.isUndefined(JSON.parse(json).id)) {
          self ! fb
        } else {
          val id = JSON.parse(json).id.toString
          context.become(standard(id, msg))
        }
      case any =>
        println("unmanaged "+any)
    }
  }  

  def standard(id: String, msg: (String) => TreeMsg): Receive = {
    tbn ! ConnMsgs.Connected
    context.parent ! msg(id)
    ;{
    case m: WebRTCMsgs.MessageToBus =>
      conn ! m
    case m: WebRTCMsgs.MessageFromBus =>
      val dyn = JSON.parse(m.txt)

      if (!js.isUndefined(dyn.updateRootAdd)) {
        context.parent ! UpdateRootAdd(dyn.updateRootAdd.toString, dyn.content.toString)
      } else if (!js.isUndefined(dyn.updateStatus)) {
        context.parent ! UpdateStatus(dyn.updateStatus)
      } else if (!js.isUndefined(dyn.updateRootRemove)) {
        context.parent ! UpdateRootRemove(dyn.updateRootRemove.toString)
      } else {
        println("ERROR cannot deserialize")
      }

      //context.parent ! m
    case WebRTCMsgs.Disconnected =>
      context.parent ! Remove(Node(id, self))
    }
  }

}
