package eu.unicredit

import akka.actor._

import upickle._
import upickle.default._

import java.util.UUID.randomUUID

object TreeMsgs {

  case class Node(id: String, channel: ActorRef) { 
    override def equals(x: Any) = {
      x match {
        case Node(xid, _) => xid == id
        case _ => false
      }
    }
  }

  class TreeMsg
  case class AddParent(node: Node) extends TreeMsg
  case class AddChild(node: Node) extends TreeMsg
  case class Remove(node: Node) extends TreeMsg

  case class UpdateRootAdd(id: String, json: String) extends WebRTCMsgs.MessageToBus(write(this))
  case class UpdateRootRemove(id: String) extends WebRTCMsgs.MessageToBus(write(this))
  case class UpdateStatus(json: String) extends WebRTCMsgs.MessageToBus(write(this))
  case class Chat(target: String, sender: String, content: String) extends WebRTCMsgs.MessageToBus(write(this))
}

case class TreeManager(name: String, connection: ActorRef) extends Actor {
  import TreeMsgs._

  val id = randomUUID.toString
  val myNode = Node(id, self)

  override def preStart() = {
    connection ! WebRTCMsgs.Assign(self)
  }

  def receive = {
    case _ =>
  }
}
