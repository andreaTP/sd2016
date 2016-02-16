package eu.unicredit.piratechat

import scala.scalajs.js
import js.Dynamic.literal
import js.JSON

import akka.actor._

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
      tbn ! GistMsgs.Publish(token)
    case j: WebRTCMsgs.Join =>
      conn ! j
    case WebRTCMsgs.Connected =>
      context.become(
        exchangeUUID((str: String) => ConnMsgs.AddChild(Node(str, self))))
  }

  def slave: Receive = {
    case WebRTCMsgs.JoinAnsware(token) =>
      tbn ! GistMsgs.AvailableAnswer(parentId, token)
    case WebRTCMsgs.Connected =>
      context.become(
        exchangeUUID((str: String) => AddParent(Node(str, self))))
  }

  def exchangeUUID(msg: (String) => TreeMsg): Receive = {

    def sendHandShake() = {
      import context._
      import scala.concurrent.duration._
      context.system.scheduler.scheduleOnce(100 millis){
        conn ! new WebRTCMsgs.MessageToBus{
          val txt = JSON.stringify(literal(id = parentId))
        }
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

      import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
      //to be imnproved
      val msg = 
        decode[ConnMsgs.UpdateRootAdd](m.txt).getOrElse(
        decode[ConnMsgs.UpdateRootRemove](m.txt).getOrElse(
        decode[ConnMsgs.UpdateStatus](m.txt).getOrElse(
        decode[ConnMsgs.Chat](m.txt).getOrElse(
        decode[ParachuteMsgs.AskForParachute](m.txt).getOrElse(
        decode[ParachuteMsgs.AskForParachute](m.txt).getOrElse(
        null))))))

      context.parent ! msg
    case WebRTCMsgs.Disconnected =>
      context.parent ! Remove(Node(id, self))
    }
  }

}
