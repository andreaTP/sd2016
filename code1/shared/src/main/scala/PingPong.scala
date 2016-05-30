package eu.unicredit

import akka.actor._

object PingPong {
  lazy val system = ActorSystem("pingpong", AkkaConfig.config)

  def ppActor(matcher: String, answerTo: ActorRef => ActorRef, answer: String) = Props(
      new Actor {
        def receive = {
          case matcher =>
            answerTo(sender) ! answer
            println(s"received $matcher sending answer $answer")
        }
      }
    ) 

  def start = {

    val ponger = system.actorOf(ppActor("ping", sender => sender, "pong"))
    val pinger = system.actorOf(ppActor("pong", _ => ponger, "ping"))

    import system.dispatcher
    import scala.concurrent.duration._
    system.scheduler.scheduleOnce(1 second)(pinger ! "pong")

    system.scheduler.scheduleOnce(2 seconds)(System.exit(0))
  }

}