package eu.unicredit

import akka.actor._
import AkkaConfig.config

import scalatags.JsDom._
import scalatags.JsDom.all._

import scala.scalajs.js

object TreeViewMsgs {

  case class SetId(id: String)
  case class NewStatus(tree: js.Dynamic)

}

case class Node(name: String, descendants: List[Node] = List())

case class TreeView() extends DomActorWithParams[Node] {
  import svgTags._
  import svgAttrs._
  import paths.high.Tree

  var myid: String = ""

  val initValue = Node("-", List())

  def template(n: Node) = {
    try {
    val tree = getTree(n)

    val tv = svg(visibility := "hidden", width := 0, height := 0)(
        g(transform := "translate(400, 50) rotate(90)")(
          (branches(tree) ++ nodes(tree)) : _*
        )
      ).render
    div(
      button(onclick := {
        () => {
          if (tv.style.visibility == "hidden") {
            tv.style.visibility = "visible"
            tv.style.width = "400"
            tv.style.height = "400"
          }
          else {
            tv.style.visibility = "hidden"
            tv.style.width = "0"
            tv.style.height = "0"
          }
        }})("show tree"),
      tv
    )
    } catch {
      case err: Throwable =>
        err.printStackTrace
        p("error")
    }
  }

  def getTree(treeNodes: Node) = 
    Tree[Node](
      data = treeNodes,
      children = _.descendants,
      width = 300,
      height = 300
    )

  private def move(p: js.Array[Double]) = {
    val p0 =
      if (p(0).toString == "NaN") 0
      else p(0)

    val p1 =
      if (p(1).toString == "NaN") 0
      else p(1)
    
    s"translate(${ p0 },${ p1 })"
  }
  private def isLeaf(node: Node) = node.descendants.length == 0  

  def branches(tree: Tree[Node]) = tree.curves map { curve =>
    path(d := curve.connector.path.print,
      stroke := "grey", 
      fill := "none"
    )
  }

  def nodes(tree: Tree[Node]) = tree.nodes map { node =>
    g(transform := move(node.point),
      circle(r := 10, cx := 0, cy := 0),
      text(
        transform := "rotate(-90) translate(20, 5)",
        textAnchor := "start",
        node.item.name
      )
    )
  }

  def fromJsonToNode(tree: js.Dynamic, id: String): Node = {
    val name = tree.selectDynamic(id).name
    val symName = 
      if (js.isUndefined(name)) {
        if (myid == id)
          "ME: "+id.toString
        else 
          id
      } else if (myid == id)
        "ME: "+name.toString
      else {
        if (name.toString == myid)
          "ME: "+name.toString
        else
          name.toString
      }

    Node(symName, tree.selectDynamic(id).sons.asInstanceOf[js.Array[String]].map(sid =>
      fromJsonToNode(tree, sid.toString)
    ).toList)
  }

  override def operative = domManagement orElse {
    case TreeViewMsgs.SetId(id) =>
      myid = id
      self ! UpdateValue(Node(id))
    case TreeViewMsgs.NewStatus(tree) =>
      self ! UpdateValue(fromJsonToNode(tree, tree.root.toString))
  }

}
