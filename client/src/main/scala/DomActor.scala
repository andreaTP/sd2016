package eu.unicredit

import akka.actor.Actor
import org.scalajs.dom.raw
import scalatags.JsDom._

import scala.scalajs.js
import js.Dynamic.{global => g, newInstance => jsnew}

object DomMsgs {
  case object NodeAsk
  case class Parent(node: raw.Node)
  case class Remove(node: raw.Node)
}

object DomDiff {
  final val dd = jsnew(g.diffDOM)().asInstanceOf[js.Dynamic]

  def apply(original: raw.Node, target: raw.Node): Unit = {
    dd.applyDynamic("apply")(original, dd.diff(original, target))
  }
}

import DomMsgs._

trait DomActor extends Actor {

  case object Update

  val domElement: Option[raw.Node] = None

  def template: TypedTag[_ <: raw.Element]

  protected var thisNode: raw.Node = _

  def receive = domRendering

  protected def initDom(p: raw.Node): Unit = {
    thisNode = template().render
    p.appendChild(thisNode)
  }

  private def domRendering: Receive = {
    domElement match {
      case Some(de) =>
        initDom(de)

        operative
      case _ =>
        context.parent ! NodeAsk

        domManagement orElse {
          case Parent(node) =>
            initDom(node)
            context.become(operative)
        }
    }
  }

  def domManagement: Receive =
    updateManagement orElse {
      case NodeAsk =>
        sender ! Parent(thisNode)
    }

  def updateManagement: Receive = {
    case Update =>
      DomDiff(thisNode, template().render)
  }

  def operative: Receive = domManagement

  override def postStop() = {
    thisNode.parentNode.removeChild(thisNode)
  }
}

trait DomActorWithParams[T] extends DomActor {

  case class UpdateValue(value: T)

  val initValue: T

  def template(): TypedTag[_ <: raw.Element] = null
  def template(value: T): TypedTag[_ <: raw.Element]

  override protected def initDom(p: raw.Node) = {
    thisNode = template(initValue).render
    p.appendChild(thisNode)
  }

  override def updateManagement: Receive = {
    case UpdateValue(newValue) =>
      DomDiff(thisNode, template(newValue).render)
  }
}
