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


  case class UpdateRootAdd(id: String, json: String) extends 
    WebRTCMsgs.MessageToBus(JSON.stringify(literal(updateRootAdd = id, content = json)))
  case class UpdateRootRemove(id: String) extends
    WebRTCMsgs.MessageToBus(JSON.stringify(literal(updateRootRemove = id)))

  case class UpdateStatus(json: js.Dynamic) extends
    WebRTCMsgs.MessageToBus(JSON.stringify(literal(updateStatus = json)))

  case class Chat(target: String, sender: String, content: String) extends
    WebRTCMsgs.MessageToBus(JSON.stringify(literal(chat = target, sender = sender, text = content)))

  case class SetName(name: String)
}

object ParachuteMsgs {

  case class AskForParachute(myId: String, targetId: String) extends
    WebRTCMsgs.MessageToBus(
      JSON.stringify(literal(
        parachuteAsk = myId,
        target = targetId
        ))
    )
  case class Parachute(myId: String, targetId: String, token: String) extends
    WebRTCMsgs.MessageToBus(
      JSON.stringify(literal(
        parachute = myId,
        target = targetId,
        token = token
        ))
    )
  case class Roger(myId: String, targetId: String, token: String) extends
    WebRTCMsgs.MessageToBus(
      JSON.stringify(literal(
        roger = myId,
        target = targetId,
        token = token
        ))
    )

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
      println("new status\n"+JSON.stringify(json))

      statusView ! PageMsgs.NewStatus(json)
      sons.foreach(s => s.channel ! msg)
      context.become(operative(parent, sons, json))
    case msg @ UpdateRootAdd(fatherId, json) =>
      parent match {
        case Some(p) => 
          p.channel ! msg
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
          val newSons = sons.filterNot(_.id == sid)

          remove(sid)(status)

          statusView ! PageMsgs.NewStatus(status)
          newSons.foreach(s => s.channel ! UpdateStatus(status))
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
      println("receiving "+m.txt)
      val dyn = JSON.parse(m.txt)

      if (!js.isUndefined(dyn.updateRootAdd)) {
        context.parent ! UpdateRootAdd(dyn.updateRootAdd.toString, dyn.content.toString)
      } else if (!js.isUndefined(dyn.updateStatus)) {
        context.parent ! UpdateStatus(dyn.updateStatus)
      } else if (!js.isUndefined(dyn.updateRootRemove)) {
        context.parent ! UpdateRootRemove(dyn.updateRootRemove.toString)
      } else if (!js.isUndefined(dyn.chat)) {
        context.parent ! Chat(dyn.chat.toString, dyn.sender.toString, dyn.text.toString)
      } 
      // parachute messages
      else if (!js.isUndefined(dyn.parachuteAsk)) {
        context.parent ! ParachuteMsgs.AskForParachute(dyn.parachuteAsk.toString, dyn.target.toString)
      } else if (!js.isUndefined(dyn.parachute)) {
        context.parent ! ParachuteMsgs.Parachute(dyn.parachute.toString, dyn.target.toString, dyn.token.toString)
      } else if (!js.isUndefined(dyn.roger)) {
        context.parent ! ParachuteMsgs.Roger(dyn.roger.toString, dyn.target.toString, dyn.token.toString)
      }

      else {
        println("ERROR cannot deserialize")
      }
      //context.parent ! m
    case WebRTCMsgs.Disconnected =>
      context.parent ! Remove(Node(id, self))
    }
  }

}
