package eu.unicredit

import akka.actor._
import scala.concurrent.duration._
import AkkaConfig.config
import raft._
import scala.util.{Success, Failure}

object Algo {

  implicit lazy val system = ActorSystem("raft", AkkaConfig.config)

  def start() = {    
    import system._
    system.scheduler.scheduleOnce(0 millis)(run)
  }

  class OutputManager extends Actor {
    def receive = {
      case UIState(id, role) =>
        println()
        role match {
          case Leader => println("NEW LEADER -> "+id)
          case _ =>
            println("NEW STATE "+role.getClass().getSimpleName.toLowerCase()+" -> "+id)
        }
      case UIHeartbeat(from) =>
        print("<3 ")
      case x =>
        println("SEQUENCE -> "+x.toString)
    }
  }

  class Sequencer(ui: ActorRef) extends Actor with RaftClient {
    import context.dispatcher

    def schedule = system.scheduler.scheduleOnce(5000 millis, self, "sequence")

    override def preStart() = schedule
    override def postRestart(reason: Throwable) = {}

    def receive = {
      case "sequence" =>
        decide("get") onComplete {
          case Success(x) => ui ! x
          case Failure(t) => println(s"Error ${t.getMessage()}")
        }

        schedule
    }
  }

  def run = {
    val ui = system.actorOf(Props(new OutputManager()), "manager")

    val members = Raft(3, ui)

    val client = system.actorOf(Props(new Sequencer(ui)), "client")
  }
}