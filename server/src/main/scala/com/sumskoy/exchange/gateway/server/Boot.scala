package com.sumskoy.exchange.gateway.server

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.sumskoy.exchange.gateway.server.tcp.TcpServerActor

object Boot extends App {
  implicit val system = ActorSystem("excahnge-gateway")
  implicit val materializer = ActorMaterializer()

  system.actorOf(TcpServerActor.props(
    system.settings.config.getString("exchange-gateway.server.tcp.host"),
    system.settings.config.getInt("exchange-gateway.server.tcp.port")
  ))
}
