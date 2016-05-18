package eu.unicredit

import akka.actor._
import scala.concurrent.duration._
import AkkaConfig.config

object PingPong {

  lazy val system = ActorSystem("pingpong", config)

  def start = {
    println("Starting ping pong!")

    val ponger = system.actorOf(Props(
      new Actor {
        def receive = {
          case "ping" =>
            println("received ping sending pong")
            sender ! "pong"
        }
      }
    ))

    val pinger = system.actorOf(Props(
      new Actor {
        def receive = {
          case "pong" =>
            println("received pong sending ping")
            ponger ! "ping"
        }
      }
    ))

    import system._
    system.scheduler.scheduleOnce(1 second)(
      pinger ! "pong"
    )

    system.scheduler.scheduleOnce(2 seconds)(System.exit(0))
  }

}