package com.sumskoy.exchange.gateway.server
package tcp

import java.net.InetSocketAddress

import akka.actor.{Actor, Props}
import akka.io.{IO, Tcp}

/**
  * TCP Server actor
  * @param host bind hostname
  * @param port bid port
  */
class TcpServerActor(host: String, port: Int) extends Actor {

  import akka.io.Tcp._
  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress(host, port))

  def receive = {
    case _: Bound =>
    case CommandFailed(_: Bind) => context stop self
    case Connected(remote, _) =>
      val connection = sender()
      val handler = context.actorOf(TcpClientActor.props(connection, remote))
      connection ! Register(handler)
  }

}

object TcpServerActor {
  def props(host: String, port: Int) = Props(new TcpServerActor(host, port))
}