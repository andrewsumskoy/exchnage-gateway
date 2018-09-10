package com.sumskoy.exchange.gateway.server
package tcp

import akka.util.ByteString
import domain._

object protocol {

  sealed trait Income

  sealed trait Outgoing

  final case class Subscribe(exchange: String, pair: String) extends Income
  final case class UnSubscribe(exchange: String, pair: String) extends Income

  final case class FullOrderBook(exchange: String, pair: String, o: OrderBook) extends Outgoing
  final case class DiffOrderBook(exchange: String, pair: String, o: OrderBookDiff) extends Outgoing

  def parse(bs: ByteString): Option[Income] = {
    val cmd = bs.utf8String
    if (cmd.startsWith("S ")) {
      val s = cmd.substring(2).split(":").toList
      if (s.size == 2) {
        Some(Subscribe(s.head, s.last))
      } else None
    } else if (cmd.startsWith("U ")) {
      val s = cmd.substring(2).split(":").toList
      if (s.size == 2) {
        Some(UnSubscribe(s.head, s.last))
      } else None
    } else None
  }

  def format(out: Outgoing): ByteString = out match {
    case FullOrderBook(ex, pair, o) =>
      ByteString(
        s"F;${o.atTime.toString()};$ex;$pair;" +
          o.orders.map(o =>s"${o.side.toString}:${o.price}:${o.originalAmount}:${o.remainingAmount}").mkString(";")
      )
    case DiffOrderBook(ex, pair, diff) =>
      ByteString(
        s"D;${diff.atTime.toString()};$ex;$pair;" + diff.updated.map { case (t, o) =>
          val ts = t match {
            case DiffType.New => "N"
            case DiffType.Updated => "U"
            case DiffType.Realized => "R"
          }
          s"$ts:${o.side.toString}:${o.price}:${o.originalAmount}:${o.remainingAmount}"
        }.mkString(";")
      )
  }
}
