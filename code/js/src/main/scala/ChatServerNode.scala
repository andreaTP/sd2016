
package eu.unicredit

import scala.scalajs.js
import js.Dynamic.{global => g}
import js.Dynamic.literal
import js.DynamicImplicits._

import ChatServer._

import akka.actor._

object ChatServerNode {

  def run = {
    ChatServer.manager

    val http = g.require("http")
    val WebSocketServer = g.require("websocket").server
      
    val server = http.createServer((request: js.Dynamic, response: js.Dynamic) => {
        response.writeHead(404)
        response.end("not available")
    })

    server.listen(8089, () => {
      js.Dynamic.newInstance(WebSocketServer)(literal(
        httpServer = server,
        autoAcceptConnections = false
      )).on("request", (request: js.Dynamic) => {
        system.actorOf(Props(WSHandler(request.accept(false, request.origin))))
      })
    })

    case class WSHandler(connection: js.Dynamic) extends Actor {

      override def preStart() = {
        connection.on("message", (message: js.Dynamic) => {
          manager ! ChatMsgs.Message(message.utf8Data.toString)
        })
        
        connection.on("close", (reasonCode: js.Dynamic, description: js.Dynamic) => {
          self ! PoisonPill
        })

        manager ! ChatMsgs.AddClient(self)
      }

      override def postStop() = {
        manager ! ChatMsgs.RemoveClient(self)
      }

      def receive = {
        case ChatMsgs.Message(txt) =>
          connection.send(txt)
      }
    }
  }
}