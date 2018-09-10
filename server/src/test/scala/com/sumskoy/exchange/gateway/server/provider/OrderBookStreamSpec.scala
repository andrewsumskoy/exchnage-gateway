package com.sumskoy.exchange.gateway.server
package provider

import domain._
import org.joda.time.DateTime
import org.knowm.xchange.currency.CurrencyPair

class OrderBookStreamSpec extends UnitSpec {
  "OrderBookStream" should "correct diff between two order books" in {
    val pair = CurrencyPair.BTC_USD
    val prev = OrderBook(DateTime.now(), List(
      LimitOrder(pair, OrderSide.Ask, 1, 2, 2),
      LimitOrder(pair, OrderSide.Ask, 2, 2, 2),
      LimitOrder(pair, OrderSide.Ask, 3, 2, 2),
      LimitOrder(pair, OrderSide.Ask, 4, 2, 2),

      LimitOrder(pair, OrderSide.Bid, 10, 2, 2),
      LimitOrder(pair, OrderSide.Bid, 12, 2, 2),
      LimitOrder(pair, OrderSide.Bid, 13, 2, 2),
      LimitOrder(pair, OrderSide.Bid, 14, 2, 2)
    ))

    OrderBookStream.diffBooks(prev, prev).updated shouldBe empty

    val next = OrderBook(DateTime.now(), List(
      LimitOrder(pair, OrderSide.Ask, 1, 2, 2),
      LimitOrder(pair, OrderSide.Ask, 2, 2, 2),
      LimitOrder(pair, OrderSide.Ask, 3, 2, 2),
      LimitOrder(pair, OrderSide.Ask, 5, 6, 7),

      LimitOrder(pair, OrderSide.Bid, 10, 2, 1),
      LimitOrder(pair, OrderSide.Bid, 12, 2, 2),
      LimitOrder(pair, OrderSide.Bid, 13, 2, 2),
      LimitOrder(pair, OrderSide.Bid, 14, 2, 2)
    ))

    OrderBookStream.diffBooks(prev, next).updated should contain only (
      DiffType.Realized -> LimitOrder(pair, OrderSide.Ask, 4, 2, 0),
      DiffType.New -> LimitOrder(pair, OrderSide.Ask, 5, 6, 7),
      DiffType.Updated -> LimitOrder(pair, OrderSide.Bid, 10, 2, 1)
    )
  }
}
