package eu.unicredit.backbone

import akka.actor.Actor

  import org.scalajs.dom.raw.{Node, Element}

object DomMsgs {
  case object NodeAsk
  case class Parent(node: Node)
  case class Remove(node: Node)
}

trait DomActor extends Actor {
  import DomMsgs._

  import scalatags.JsDom._
  import scalatags.JsDom.all._

  case object Update

  val domElement: Option[Node] = None

  def template: TypedTag[_ <: Element]

  protected var thisNode: Node = _

  def receive = domRendering

  protected def initDom(p: Node): Unit = {
    thisNode = template().render
    p.appendChild(thisNode)
  }

  private def domRendering: Receive = {
    domElement match {
      case Some(de) =>
        val parent = de.parentNode
        parent.removeChild(de)
        initDom(parent)

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
      case Remove(child) =>
        thisNode.removeChild(child)
    }

  def updateManagement: Receive = {
    case Update =>
      val p = thisNode.parentNode
      p.removeChild(thisNode)

      thisNode = template().render

      p.appendChild(thisNode)
  }

  def operative: Receive = domManagement

  override def postStop() = {
    context.parent ! Remove(thisNode)
  }
}

trait DomActorWithParams[T] extends DomActor {
  import DomMsgs._

  import scalatags.JsDom._
  import scalatags.JsDom.all._

  case class UpdateValue(value: T)

  val initValue: T

  def template(): TypedTag[_ <: Element] = null
  def template(value: T): TypedTag[_ <: Element]

  override protected def initDom(p: Node) = {
    thisNode = template(initValue).render
    p.appendChild(thisNode)
  }

  override def updateManagement: Receive = {
    case UpdateValue(newValue) =>
      val p = thisNode.parentNode
      p.removeChild(thisNode)

      thisNode = template(newValue).render

      p.appendChild(thisNode)
  }
}
