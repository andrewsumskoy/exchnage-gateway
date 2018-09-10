package com.sumskoy.exchange.gateway.server
package tcp

import domain.OrderBookDiff
import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp
import akka.util.ByteString
import com.sumskoy.exchange.gateway.server.provider.OrderBookProvider

import scala.annotation.tailrec

/**
  * Actor for each tcp client
  * @param connection original connection actor
  * @param remote remote client address
  */
class TcpClientActor(connection: ActorRef, remote: InetSocketAddress) extends Actor with ActorLogging {

  import TcpClientActor._
  import akka.io.Tcp._

  val provider = OrderBookProvider(context.system)

  val RN = ByteString("\r\n")

  var buffer = ByteString()
  var readPrev = 0
  var writeBuffer = ByteString()
  var waitAck = false
  val maxWriteSize: Int = 1024 * 1024
  val writeChunkSize: Int = 16 * 1024

  var firstData = Map.empty[(String, String), Boolean]

  def receive = {
    case Received(data) =>
      buffer ++= data
      read()
    case Ack(size) =>
      waitAck = false
      writeBuffer = writeBuffer.drop(size)
      if (writeBuffer.nonEmpty) writeChunk()
    case ('command, cmd: ByteString) => onCommand(cmd)
    case (ex: String, pair: String, o: OrderBookDiff) =>
      if (firstData.getOrElse(ex -> pair, true)) {
        firstData += (ex -> pair) -> false
        write(protocol.format(protocol.FullOrderBook(ex, pair, o.next)))
      } else if (o.updated.nonEmpty) {
         write(protocol.format(protocol.DiffOrderBook(ex, pair, o)))
      }
    case PeerClosed => context stop self
  }

  def onCommand(cmd: ByteString): Unit = protocol.parse(cmd) match {
    case Some(protocol.Subscribe(ex, pair)) => provider.subscribe(ex, pair, self)
    case Some(protocol.UnSubscribe(ex, pair)) =>
      provider.unsubscribe(ex, pair, self)
      firstData += (ex, pair) -> true
    case _ =>
      log.warning(s"Unknown income command ${cmd.utf8String} from $remote")
  }

  def write(s: ByteString): Unit = {
    writeBuffer ++= s ++ ByteString("\r\n")
    if (writeBuffer.size > maxWriteSize) {
      log.error(s"Write buffer too high ${writeBuffer.size}. reconnect")
      connection ! Close
    }
    if (writeBuffer.nonEmpty && !waitAck) writeChunk()
  }

  def writeChunk(): Unit = {
    if (writeBuffer.nonEmpty) {
      val chunk = writeBuffer.take(writeChunkSize)
      connection ! Write(chunk, Ack(chunk.size))
      waitAck = true
    }
  }


  @tailrec final def read(): Unit = {
    val delimeter = buffer.indexOf(RN.head, from = readPrev)
    if (delimeter == -1) {
      readPrev = buffer.size
    } else if (delimeter + 1 > buffer.size) {
      // not enough bytes
    } else if (buffer.slice(delimeter, delimeter + RN.size) == RN) {
      val income = buffer.slice(0, delimeter)
      buffer = buffer.drop(delimeter + RN.size)
      readPrev = 0
      self ! 'command -> income
      read()
    } else {
      readPrev = buffer.size
      read()
    }
  }

}

object TcpClientActor {
  def props(connection: ActorRef, remote: InetSocketAddress) = Props(new TcpClientActor(connection, remote))

  final case class Ack(offset: Int) extends Tcp.Event

}
