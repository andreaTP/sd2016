package eu.unicredit

import ChatServer._

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.handleWebSocketMessages
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl._
import akka.stream._
import akka.stream.actor._

object ChatServerAkkaHttp {

  def run = {
    implicit val flowMaterializer = ActorMaterializer()
    import system.dispatcher

    case class SourceWSHandler() extends ActorPublisher[Message] {
      import ActorPublisherMessage._

      override def preStart() =
        ChatServer.manager ! ChatMsgs.AddClient(self)

      def receive = {
        case msg: ChatMsgs.Message =>
          onNext(TextMessage(msg.value))
      }

      override def postStop() =
        ChatServer.manager ! ChatMsgs.RemoveClient(self)
    }

    case class SinkWSHandler() extends ActorSubscriber {
      import ActorSubscriberMessage._
      override val requestStrategy = new MaxInFlightRequestStrategy(max = 1) {
        override def inFlightInternally: Int = 0
      }
      
      def receive = {
        case OnNext(any: Any) =>
          any match {
            case TextMessage.Strict(text) =>
              ChatServer.manager ! ChatMsgs.Message(text)
          }
      }
    }

    def actorSource = Source.actorPublisher[Message](Props(new SourceWSHandler()))
    def actorSink = Sink.actorSubscriber(Props(new  SinkWSHandler()))

    def msgFlow(): Flow[Message,Message,Any] =
      Flow.fromSinkAndSource(actorSink, actorSource)
     
    val route =
      path("") {
        get {
            handleWebSocketMessages(msgFlow)
        }
      }
   
    val bindingFuture = Http().bindAndHandle(route, "localhost", 8098)
   
  }
}
