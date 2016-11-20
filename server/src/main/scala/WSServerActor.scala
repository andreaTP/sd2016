package eu.unicredit

import scala.scalajs.js
import js.Dynamic.{global => g, literal}

import akka.actor._

class WSServerActor(port: Int) extends Actor {

  val http = g.require("http")
  val WebSocketServer = g.require("websocket").server

  val server =
    http.createServer((request: js.Dynamic, response: js.Dynamic) => {
      response.writeHead(404)
      response.end("not available")
    })

  val wsServer =
    js.Dynamic.newInstance(WebSocketServer)(
      literal(
        httpServer = server,
        keepaliveInterval = 1000,
        keepaliveGracePeriod = 3000,
        autoAcceptConnections = false
      ))

  wsServer.on("request", (request: js.Dynamic) => {
    context.actorOf(
      Props(new WSTwitterChannelActor(request.accept(false, request.origin))))
  })

  server.listen(port, () => wsServer)

  def receive = { case _ => }
}
