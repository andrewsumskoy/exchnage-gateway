package com.sumskoy.exchange.gateway.server
package domain

import org.joda.time.DateTime

object DiffType extends Enumeration {
  val Realized, Updated, New = Value
}

case class OrderBook(atTime: DateTime, orders: Seq[LimitOrder], levels: Map[(BigDecimal, OrderSide.Value), LimitOrder])

case class OrderBookDiff(atTime: DateTime, prev: OrderBook, next: OrderBook, updated: List[(DiffType.Value, LimitOrder)])

object OrderBook {
  def empty = OrderBook(DateTime.now(), Seq.empty, Map.empty)

  def apply(atTime: DateTime, orders: Seq[LimitOrder]): OrderBook = {
    val levels = orders.groupBy(el => el.price -> el.side).mapValues { values =>
      LimitOrder(
        values.last.pair,
        values.last.side,
        price = values.last.price,
        originalAmount = values.map(_.originalAmount).sum,
        remainingAmount = values.map(_.remainingAmount).sum
      )
    }
    OrderBook(atTime, orders, levels)
  }
}
