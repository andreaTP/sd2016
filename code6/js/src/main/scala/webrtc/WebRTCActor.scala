package eu.unicredit

import akka.actor._

import scala.util.{Success, Failure}

import scala.scalajs.js

import org.scalajs.dom.raw._
import org.scalajs.dom.experimental.webrtc._

import upickle._
import upickle.default._

import java.util.UUID.randomUUID

object WebRTCMsgs {

  sealed trait Command
  case object Create extends Command
  case class Join(token: String) extends Command

  case class OfferToken(token: String) extends Command
  case class JoinToken(token: String) extends Command

  case object Connected extends Command
  case object Disconnected extends Command

  case class Assign(manager: ActorRef) extends Command

  class MessageFromBus(val txt: String)
  class MessageToBus(val txt: String)
}

case class WebRTCConnection(name: String, ui: ActorRef) extends Actor {
  import WebRTCMsgs._
  import Serializations._
  import context.dispatcher

  case class MessageFromBus(txt: String)

  val stuns: js.Array[String] =
    //debug mode
    js.Array("localhost")

  val servers =
    stuns.map(url => RTCIceServer(urls = s"stun:$url"))

  val offerOptions =
    RTCOfferOptions(
      iceRestart = false,
      offerToReceiveAudio = 0,
      offerToReceiveVideo = 0,
      voiceActivityDetection = false)

  val channelOptions =
    RTCDataChannelInit()

  val connection =
    new RTCPeerConnection(
      RTCConfiguration(
      iceServers = servers,
      iceTransportPolicy = RTCIceTransportPolicy.all,
      bundlePolicy = RTCBundlePolicy.balanced)
    )

  connection.onicecandidate =
    (e: RTCPeerConnectionIceEvent) => {
      if (e.candidate == null)
        self ! connection.localDescription
    }

  def receive = {
    case Create =>
      println("have to create a new connection")
      context.become(connect)
    case Join(token) =>
      println("have to join connection")
      context.become(join(read[RTCSessionDescription](token)))
  }

  def bindChannel(channel: RTCDataChannel) = {
    channel.onopen = (e: Event) => ()

    channel.onclose = (e: Event) => {
      context.parent ! Disconnected
      self ! PoisonPill
    }

    channel.onerror = (e: Event) => {
      context.parent ! Disconnected
      self ! PoisonPill
    }

    channel.onmessage = (e: MessageEvent) => {
      println("RECEIVED MESSAGE "+e.data)
      self ! MessageFromBus(e.data.toString)
    }
  }

  def connect: Receive = {

    val channel = 
      connection.createDataChannel(randomUUID().toString, channelOptions)

    bindChannel(channel)

    connection.createOffer(offerOptions).toFuture.onComplete{
      case Success(desc: RTCSessionDescription) =>
        connection.setLocalDescription(desc)
      case Failure(err) =>
        println(s"Couldn't create offer $err")
    }
    
    ;{
      case desc: RTCSessionDescription =>
        ui ! OfferToken(write[RTCSessionDescription](desc))
        context.become(waitingForClient(channel))
    }
  }

  def waitingForClient(channel: RTCDataChannel): Receive = {
    case JoinToken(token) =>
      read[RTCSessionDescription](token)
      connection.setRemoteDescription(read[RTCSessionDescription](token))
      context.become(connected(channel))
  }

  def join(offerDesc: RTCSessionDescription): Receive = {
    case class Channel(c: RTCDataChannel)

    connection.ondatachannel = (e: RTCDataChannelEvent) => {
      bindChannel(e.channel)
      self ! Channel(e.channel)
    }

    connection.setRemoteDescription(offerDesc)

    connection.createAnswer.toFuture.onComplete{
      case Success(desc: RTCSessionDescription) =>
        connection.setLocalDescription(desc)
      case Failure(err) =>
        println(s"Couldn't create answer $err")
    }

    ;{
      case desc: RTCSessionDescription =>
        ui ! JoinToken(write(desc))
      case Channel(channel) =>
        context.become(connected(channel))
    }
  }

  def connected(channel: RTCDataChannel): Receive = {
    ui ! Connected

    ;{
      case Assign(manager) =>
        context.become(operative(channel, manager))
    }
  }

  def operative(channel: RTCDataChannel, manager: ActorRef): Receive = {
    case msg: MessageFromBus =>
      manager ! msg
    case msg: MessageToBus =>
      channel.send(msg.txt)
  }

}

object StunServers {

  def servers =
  //real world
  js.Array(
    "stun.l.google.com:19302",
    "stun1.l.google.com:19302",
    "stun2.l.google.com:19302",
    "stun3.l.google.com:19302",
    "stun4.l.google.com:19302",
    "stun01.sipphone.com",
    "stun.ekiga.net",
    "stun.fwdnet.netv",
    "stun.ideasip.com",
    "stun.iptel.org",
    "stun.rixtelecom.se",
    "stun.schlund.de",
    "stunserver.org",
    "stun.softjoys.com",
    "stun.voiparound.com",
    "stun.voipbuster.com",
    "stun.voipstunt.com",
    "stun.voxgratia.org",
    "stun.xten.com"
    )
}
