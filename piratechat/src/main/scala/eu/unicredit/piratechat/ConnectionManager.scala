package eu.unicredit.piratechat

import scala.scalajs.js
import js.Dynamic.literal
import js.JSON

import akka.actor._

import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

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


  case class UpdateRootAdd(id: String, json: String) extends WebRTCMsgs.MessageToBus{val txt = this.asJson.noSpaces}
  case class UpdateRootRemove(id: String) extends WebRTCMsgs.MessageToBus{val txt = this.asJson.noSpaces}
  case class UpdateStatus(json: String) extends WebRTCMsgs.MessageToBus{val txt = this.asJson.noSpaces}
  case class Chat(target: String, sender: String, content: String) extends WebRTCMsgs.MessageToBus{val txt = this.asJson.noSpaces}

  case class SetName(name: String)
}

object ParachuteMsgs {

  case class AskForParachute(myId1: String, targetId: String) extends WebRTCMsgs.MessageToBus{val txt = this.asJson.noSpaces}
  case class Parachute(myId2: String, targetId: String, token: String) extends WebRTCMsgs.MessageToBus{val txt = this.asJson.noSpaces}
  case class Roger(myId3: String, targetId: String, token: String) extends WebRTCMsgs.MessageToBus{val txt = this.asJson.noSpaces}

  case class AddParachuteHandler(id: String, ref: ActorRef)

  case class Fire(id: String)
}

case class ParachuteBox() extends Actor {
  import ParachuteMsgs._

  def receive = operative(Map())

  def operative(parachutes: Map[String, ActorRef]): Receive = {
    case AddParachuteHandler(id, ref) =>
      context.become(operative(parachutes + (id -> ref)))
    case roger @ Roger(id, _, _) =>
      parachutes.get(id).map(_ ! roger)
    case fire @ Fire(id) =>
      parachutes.get(id).map(_ ! fire)
  }
}

case class ParachuteHandler(id: String, sonId: String, sonChannel: ActorRef) extends Actor {
  import ParachuteMsgs._

  def receive = {
    case ConnMsgs.Token(token) =>
      val originalSender = sender
      sonChannel ! Parachute(id, sonId, token)
      context.become(waitingAnswer(originalSender))
  }

  def waitingAnswer(connection: ActorRef): Receive = {
    case Roger(_, _, answerToken) =>
      context.become(waitingToOpenIt(connection, answerToken)) 
  }

  def waitingToOpenIt(connection: ActorRef, token: String): Receive = {
    println("waiting to open for "+sonId)

  ;{
    case Fire(_) =>
      connection ! WebRTCMsgs.Join(token)
    }
  }
}

case class Parachuter(id: String, granpaId: String, fatherChannel: ActorRef) extends Actor {
  import ParachuteMsgs._

  def receive = {
    case ConnMsgs.Token(token) =>
      fatherChannel ! Roger(id, granpaId, token)
  }
}

class ConnManager(id: String, statusView: ActorRef) extends Actor with JsonTreeHelpers {
  import ConnMsgs._

  def receive = {
    val status = emptyRoot(id)
    statusView ! PageMsgs.NewStatus(status)

    operative(None, Seq(), status)
  }

  val pbox = context.actorOf(Props(ParachuteBox()))

  def operative(
      parent: Option[Node],
      sons: Seq[Node],
      status: js.Dynamic): Receive = {
    case Open =>
      val originalSender = sender
      val conn = context.actorOf(Props(WebRTCActor(id, originalSender)))
      conn ! WebRTCMsgs.Create
    case Attach(token) =>
      if (parent.isEmpty) {
        val originalSender = sender
        val conn = context.actorOf(Props(WebRTCActor(id, originalSender)))
        conn ! WebRTCMsgs.Join(token)
      }
    case AddParent(ref) =>
      if (js.isUndefined(status.selectDynamic(ref.id))) {
        ref.channel ! UpdateRootAdd(ref.id, JSON.stringify(status))
        context.become(operative(Some(ref), sons, status))
      } else {
        println("parent was in the status "+status)
        ref.channel ! PoisonPill
      }
    case AddChild(ref) =>
      if (js.isUndefined(status.selectDynamic(ref.id))) {

        //parachute
        parent.map(p =>
          p.channel ! ParachuteMsgs.AskForParachute(ref.id, p.id)
        )

        context.become(operative(parent, sons :+ ref, status))
      } else {
        println("child was in the status "+status)
        ref.channel ! PoisonPill
      }
    case Remove(ref) =>
      checkAndFire(status, ref.id)

      (parent) match {
        case Some(p) if (p.id != ref.id) =>
          p.channel ! UpdateRootRemove(ref.id)
        case _ =>
          self ! UpdateRootRemove(ref.id)
      }
    case msg @ UpdateStatus(json) =>
      //println("new status\n"+json)

      statusView ! PageMsgs.NewStatus(JSON.parse(json))
      sons.foreach(s => s.channel ! msg)
      context.become(operative(parent, sons, JSON.parse(json)))
    case msg @ UpdateRootAdd(fatherId, json) =>
      parent match {
        case Some(p) => 
          p.channel ! msg
        case _ =>

          merge(fatherId)(status, JSON.parse(json))

          println("STATUS UPDATE")
          statusView ! PageMsgs.NewStatus(status)
          sons.foreach(s => s.channel ! UpdateStatus(json))
      }
    case msg @ UpdateRootRemove(sid) =>

      parent match {
        case Some(p) =>
          if (p.id == sid) {

            keep(id)(status)

            statusView ! PageMsgs.NewStatus(status)
            sons.foreach(s => s.channel ! UpdateStatus(JSON.stringify(status)))
            context.become(operative(None, sons, status))
          } else {
            p.channel ! msg
          }
        case _ =>
          val newSons = sons.filterNot(_.id == sid)

          remove(sid)(status)

          statusView ! PageMsgs.NewStatus(status)
          newSons.foreach(s => s.channel ! UpdateStatus(JSON.stringify(status)))
          context.become(operative(parent, newSons, status))
      }

    //chat ui related
    case msg @ Chat(target, sender, content) =>
      if (target == id) {
        context.parent ! PageMsgs.ChatMsg(sender, content)
      } else if (isSonOf(target, id)(status)) {
        sons.foreach(s =>
          if (target == s.id || isSonOf(target,s.id)(status)) {
            s.channel ! msg
        })
      } else {
        parent.map(_.channel ! msg)
      }
    case SetName(name) =>
      val me = status.selectDynamic(id)
      me.updateDynamic("name")(name)
      status.updateDynamic(id)(me)
      statusView ! PageMsgs.NewStatus(status)

    //parchute related
    case msg @ ParachuteMsgs.AskForParachute(sonId, targetId) =>
      if (id != targetId)
        parent.map(_.channel ! msg)
      else {
        val originalSender = sender
        val parachuteHandler =
          context.actorOf(Props(ParachuteHandler(id, sonId, originalSender)))
        val conn =
          context.actorOf(Props(WebRTCActor(id, parachuteHandler)))

        pbox ! ParachuteMsgs.AddParachuteHandler(sonId, parachuteHandler)

        conn ! WebRTCMsgs.Create
      }
    case msg @ ParachuteMsgs.Parachute(granpaId, targetId, token) =>
      if (id != targetId)
        sons.foreach(s => if (s.id == targetId) s.channel ! msg)
      else {
        val originalSender = sender
        val parachuter =
          context.actorOf(Props(Parachuter(id, granpaId, originalSender)))
        val conn =
          context.actorOf(Props(WebRTCActor(id, parachuter)))
        conn ! WebRTCMsgs.Join(token)
      }
    case msg @ ParachuteMsgs.Roger(sonId, targetId, token) =>
      if (id != targetId)
        parent.map(_.channel ! msg)
      else
        pbox ! msg
    case _ =>
  }

  def checkAndFire(status: js.Dynamic, sid: String) = {
      //Open the parachute if needed
      //get the nephew
      val nephews = {
        try {
          val n = status.selectDynamic(sid).sons
          n.asInstanceOf[js.Array[String]]
        } catch {
          case _ : Throwable =>
            js.Array[String]()
        }
      }

      //println("status is "+JSON.stringify(status))
      println("remove nephews for id "+sid+" are "+nephews)

      nephews.foreach(n => {
        println("have to fire "+n)
        pbox ! ParachuteMsgs.Fire(n)
      })
  }

}

