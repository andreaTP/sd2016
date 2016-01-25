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
    WebRTCMsgs.MessageToBus(JSON.stringify(literal(updateStatus = JSON.stringify(json))))
}

class ConnManager() extends Actor {
  import ConnMsgs._

  val id = java.util.UUID.randomUUID.toString

  println("I am "+id)

  def receive = operative(None, Seq(), literal(root = id))

  val conn = context.actorOf(Props(WebRTCActor(id, context.parent)))

  def operative(
      parent: Option[Node],
      sons: Seq[Node],
      status: js.Dynamic): Receive = {
    case Open =>
      conn ! WebRTCMsgs.Create
    case Attach(token) =>
      println("attaching")
      conn ! WebRTCMsgs.Join(token)
    case AddParent(ref) =>
      println("add parent")

      //devo serializzare e deserializzare gli update che vanno sul channel

      ref.channel ! UpdateRootAdd(ref.id, JSON.stringify(status))
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

          println("OK SONO LA ROOT E DEVO FARE UPDATE "+sid+" -> "+json)


          val newStatus = literal(pippo = "YEA")//status
          sons.foreach(s => s.channel ! UpdateStatus(newStatus))
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
          println("YEAAAAAA"+json)
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
      println("sending messge to bus")
      conn ! m
    case m: WebRTCMsgs.MessageFromBus =>
      println("go on from here deserializing!!! "+ m.txt)
      val dyn = JSON.parse(m.txt)

      if (!js.isUndefined(dyn.updateRootAdd)) {
        println("parsing add")
        context.parent ! UpdateRootAdd(dyn.updateRootAdd.toString, JSON.stringify(dyn.content.toString))
      }

      //context.parent ! m
    case WebRTCMsgs.Disconnected =>
      context.parent ! Remove(Node(id, self))
    }
  }

}
