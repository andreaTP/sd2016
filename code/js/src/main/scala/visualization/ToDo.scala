package eu.unicredit

import akka.actor._
import AkkaConfig.config

import org.scalajs.dom.document.{getElementById => getElem}

import scalatags.JsDom._
import scalatags.JsDom.all._

object ToDo {

  implicit lazy val system = ActorSystem("todo", config)

  def start =
    system.actorOf(Props(ToDoList()), "page")
    //system.actorOf(Props(ToDoList2()), "page")

  case class ToDoList() extends DomActor {
    override val domElement = Some(getElem("root"))

    val inputBox = input("placeholder".attr := "what to do?").render

    def template() = ul(
        h1("ToDo Demo!"),
        div(
          inputBox,
          button(onclick := {
            () => context.actorOf(Props(ToDoElem(inputBox.value)))})(
            "Add"
          )
        )
      )
  }

  case class ToDoElem(value: String) extends DomActor {
    def template() = div(
      li(value),
      button(onclick := {
        () => self ! PoisonPill})(
        "Remove"
      )
    )
  }
/*
  case class ToDoList2() extends DomActorWithParams[List[String]] {
    override val domElement = Some(getElem("root"))

    val inputBox = input("placeholder".attr := "what to do?").render

    val initValue = List()

    def template(list: List[String]) =
      ul(
        h1("ToDo Demo!"),
        div(
          inputBox,
          button(onclick := {
            () => self ! UpdateValue(list :+ inputBox.value)})(
            "Add"
          ),
          for (item <- list) yield {
            div(
              li(item),
              button(onclick := {
                () => self ! UpdateValue(list.filterNot(_ == item))})(
                "Remove"
              )
            )
          }
        )
      )
  }
*/
}
