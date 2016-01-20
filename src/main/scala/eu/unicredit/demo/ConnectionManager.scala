package eu.unicredit.demo

import akka.actor._

object ConnMsgs {

  case object Open
  case class Attach(token: String)

  case class Token(token: String)

  case class AddParent(ref: ActorRef)
  case class AddChild(ref: ActorRef)
  case class Remove(ref: ActorRef)
}

class ConnManager() extends Actor {

  def receive = operative(None, Seq())

  def operative(parent: Option[ActorRef], sons: Seq[ActorRef]): Receive = {
    case ConnMsgs.Open =>
      val conn = context.actorOf(Props(WebRTCActor(sender)))
      conn ! WebRTCMsgs.Create
    case ConnMsgs.Attach(token) =>
      val conn = context.actorOf(Props(WebRTCActor(sender)))
      conn ! WebRTCMsgs.Join(token)
    case ConnMsgs.AddParent(ref) =>
      context.become(operative(Some(ref), sons))
    case ConnMsgs.AddChild(ref) =>
      context.become(operative(parent, sons :+ ref))
    case ConnMsgs.Remove(ref) =>
      (parent) match {
        case Some(p) if (p == ref) =>
          context.become(operative(None, sons))
        case _ =>
          context.become(operative(parent, sons.filterNot(_ == ref)))
      }
    case _ =>
  }
}

case class WebRTCActor(tbn: ActorRef) extends Actor {

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
    case WebRTCMsgs.Connected =>
      context.parent ! ConnMsgs.AddChild(self)
      context.become(standard)
    case WebRTCMsgs.Disconnected =>
      context.parent ! ConnMsgs.Remove(self)
  }

  def slave: Receive = {
    case WebRTCMsgs.JoinAnsware(token) =>
      tbn ! ConnMsgs.Token(token)
    case WebRTCMsgs.Connected =>
      context.parent ! ConnMsgs.AddParent(self)
      context.become(standard)
    case WebRTCMsgs.Disconnected =>
      context.parent ! ConnMsgs.Remove(self)
  }

  def standard: Receive = {
    case m: WebRTCMsgs.MessageToBus =>
      conn ! m
    case m: WebRTCMsgs.MessageFromBus =>
      context.parent ! m
    case WebRTCMsgs.Disconnected =>
      context.parent ! ConnMsgs.Remove(self)
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
