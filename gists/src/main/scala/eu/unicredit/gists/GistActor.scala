package eu.unicredit.gists

import scala.util.Random

import scala.scalajs.js
import js.Dynamic.literal
import js.JSON

import org.scalajs.dom.ext.Ajax

import scala.concurrent.duration._

import akka.actor._

import java.util.Base64

object GistMsgs {

  case class Publish(token: String)
  case object Listen

  case class AvailableAnswer(id: String, token: String)

  case class AvailableOffer(id: String, token: String)
  case class AnswerForOffer(id: String, token: String)

}

case class GistActor(user: String) extends Actor {
  import context._
  import GistMsgs._

  case class Published(id: String)
  case class Answer(str: String)
  case class FoundOffer(id: String)
  case class OfferToken(str: String)
  case object DeletedGists
  case object AnswerPublished
  case object Timeout
  case object Error

  val DESCRIPTION = "piratechat"

  val authHeader = Map(
    "Authorization" -> s"Basic $user")

  def nocache() =
      "?"+java.util.UUID.randomUUID().toString

  def receive = waiting

  def gistCreate(contentStr: String) =
    literal(
      description = DESCRIPTION,
      public = true,
      files = literal(
        offer = literal(
          content = contentStr
        )
      )
    )

  def waiting: Receive = {
    case Publish(str) =>
      val msg = JSON.stringify(gistCreate(str))

      Ajax.post(s"https://api.github.com/gists${nocache()}",
        msg,
        headers =authHeader,
        timeout = 2000
      ).map(req => {
        val json = JSON.parse(req.responseText)
        
        if (req.status == 201)
          self ! Published(json.id.toString)
        else
          self ! Error
      }).recover{case _ => self ! Error}
      context.become(
        waitPublished(context.system.scheduler.scheduleOnce(5 seconds){
          self ! Timeout
        })
      )
    case Listen =>
      context.become(waitOffer())
  }

  def waitPublished(timeout: Cancellable): Receive = {
    case Published(id) =>
      timeout.cancel
      context.become(monitorForAnswer()(id))
    case Timeout =>
      context.become(waiting)
    case Error =>
      self ! PoisonPill
  }

  val monitorMaxRetry = 30

  def monitorTimeout() =
    context.system.scheduler.scheduleOnce(10 seconds){
      self ! Timeout
    }

  def monitorForAnswer(timeout: Cancellable = monitorTimeout(),
      retry: Int = monitorMaxRetry)(
      implicit id: String): Receive = {
    if (retry <= 0) {
      timeout.cancel
      self ! Error
    }

    Ajax.get(s"https://api.github.com/gists/$id${nocache()}",
      headers = authHeader,
      timeout = 2000
    ).map(req => {
      val json = JSON.parse(req.responseText)
      
      if (req.status == 200) {
        try {
          self ! Answer(json.files.answer.content.toString)
        } catch {
          case _ : Throwable =>
        }
      }
    })

    {
      case Answer(token) =>
        timeout.cancel

        context.parent ! AvailableAnswer(id, token)
        
        context.become(deleteGists(id))
      case Timeout =>
        context.become(monitorForAnswer(retry =  retry - 1))
      case Error =>
        self ! PoisonPill
    }
  }

  def deleteGistsTimeout() =
    context.system.scheduler.scheduleOnce(3 seconds){
      self ! Timeout
    }

  def deleteGists(id: String,
      timeout: Cancellable = deleteGistsTimeout): Receive = {

    Ajax.delete(s"https://api.github.com/gists/$id${nocache()}",
      headers = authHeader,
      timeout = 2000
    ).map(req => {
      val json = JSON.parse(req.responseText)
      
      if (req.status == 204) {
        self ! DeletedGists
      }
    })

    {
      case DeletedGists =>
        timeout.cancel
        context.become(waitForConnection())
      case Timeout =>
        context.become(waitForConnection())
      case Error =>
        self ! PoisonPill
    }
  }

  val waitOfferMaxRetry = 100

  def waitOfferTimeout() =
    context.system.scheduler.scheduleOnce(10 seconds){
      self ! Timeout
    }

  def waitOffer(timeout: Cancellable = waitOfferTimeout(),
      retry: Int = waitOfferMaxRetry): Receive = {

    Ajax.get(s"https://api.github.com/gists/public${nocache()}",
      headers = authHeader,
      timeout = 2000
    ).map(req => {
      if (req.status == 200) {
        val json = JSON.parse(req.responseText)

        val relevantGists =
          json.asInstanceOf[js.Array[js.Dynamic]].filter(g => {
            try (
              g.description.toString == DESCRIPTION &&
              (js.Object.keys(g.files.asInstanceOf[js.Object]).size == 1)
            ) catch {
              case _ : Throwable => false
            }
          })

        val choosen = relevantGists(Random.nextInt(relevantGists.size))

        self ! FoundOffer(choosen.id.toString)
      }
    })

    {
      case FoundOffer(id) =>
        timeout.cancel
        context.become(getOfferToken(id))
      case Timeout =>
        context.become(waitOffer(retry = retry - 1))
      case Error =>
        self ! PoisonPill
    }
  }

  def getOfferTokenTimeout() =
    context.system.scheduler.scheduleOnce(3 seconds){
      self ! Timeout
    }

  def getOfferToken(id: String,
      timeout: Cancellable = waitOfferTimeout()): Receive = {

    Ajax.get(s"https://api.github.com/gists/$id${nocache()}",
      headers = authHeader,
      timeout = 2000
    ).map(req => {
      if (req.status == 200) {
        val json = JSON.parse(req.responseText)

        try {
          self ! OfferToken(json.files.offer.content.toString)
        } catch {
          case _ : Throwable =>
        }
      }
    })    

    {
      case OfferToken(token) =>
        timeout.cancel
        context.become(tryAnswer(id, token))
      case Timeout =>
        context.become(waitOffer())
      case Error =>
        self ! PoisonPill
    }
  }

  def tryAnswerTimeout() =
    context.system.scheduler.scheduleOnce(10 seconds){
      self ! Timeout
    }

  def tryAnswer(id: String,
      offerToken: String,
      timeout: Cancellable = waitOfferTimeout()): Receive = {

    context.parent ! AvailableOffer(id, offerToken)

    {
      case AnswerForOffer(aid, answer) =>
        if (id == aid) {
          timeout.cancel
          context.become(publishAnswer(id, answer))
        }
      case Timeout =>
        context.become(waitOffer())
      case Error =>
        self ! PoisonPill
    }
  }

  def gistAnswer(contentStr: String) =
    literal(
      description = DESCRIPTION,
      files = literal(
        answer = literal(
          content = contentStr
        )
      )
    )

  def publishAnswerTimeout() =
    context.system.scheduler.scheduleOnce(3 seconds){
      self ! Timeout
    }

  def publishAnswer(id: String,
      answer: String,
      timeout: Cancellable = publishAnswerTimeout()): Receive = {

    Ajax.apply("PATCH",
      s"https://api.github.com/gists/$id${nocache()}",
      data = Ajax.InputData.str2ajax(
        JSON.stringify(gistAnswer(answer))),
      timeout = 2000,
      headers = authHeader,
      withCredentials = false,
      responseType = ""
    ).map(req => {
      if (req.status == 200) {
        self ! AnswerPublished
      }
    })

    {
      case AnswerPublished =>
        timeout.cancel
        context.become(waitForConnection())
      case Timeout =>
        context.become(waitOffer())
      case Error =>
        self ! PoisonPill
    }
  }

  def waitForConnectionTimeout() =
    context.system.scheduler.scheduleOnce(30 seconds){
      self ! Timeout
    }

  def waitForConnection(timeout: Cancellable = waitForConnectionTimeout()): Receive = {
    case Timeout =>
      context.become(waitOffer())
    case Error =>
      self ! PoisonPill
  }
  
}