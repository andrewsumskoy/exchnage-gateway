package com.sumskoy.exchange.gateway.server
package provider

import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import com.sumskoy.exchange.gateway.server.domain._
import org.joda.time.DateTime
import org.knowm.xchange.Exchange
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.dto.marketdata

import scala.collection.immutable
import scala.collection.JavaConverters._
import scala.concurrent.duration._

/**
  * OrderBookStream provide stream source and mapping for retry order book and transform
  */
object OrderBookStream {
  def exchange(exchange: Exchange, pair: CurrencyPair, interval: FiniteDuration = 1.second): Source[marketdata.OrderBook, Cancellable] = {
    val marketDataService = exchange.getMarketDataService
    Source.tick(0.seconds, interval, Unit).map { _ =>
      val orderBook = marketDataService.getOrderBook(pair)
      orderBook
    }
  }

  def diffSate(source: Source[marketdata.OrderBook, Cancellable]): Source[OrderBookDiff, Cancellable] = {
    source.map { ob =>
      OrderBook(new DateTime(ob.getTimeStamp),
        ob.getAsks.asScala.toList.map(LimitOrder(OrderSide.Ask, _)) ++
          ob.getBids.asScala.toList.map(LimitOrder(OrderSide.Bid, _)))
    }.statefulMapConcat[OrderBookDiff] { () =>
      var state: OrderBook = OrderBook.empty
      (book: OrderBook) => {
        val ret = immutable.Seq(diffBooks(state, book))
        state = book
        ret
      }
    }
  }

  def diffBooks(prev: OrderBook, next: OrderBook): OrderBookDiff = {
    val changes = prev.levels.toList.collect {
      case (level, order) if !next.levels.contains(level) => DiffType.Realized -> order.copy(remainingAmount = 0d)
      case (level, order) if next.levels(level).remainingAmount != order.remainingAmount => DiffType.Updated -> next.levels(level)
    } ++ next.levels.toList.collect {
      case (level, order) if !prev.levels.contains(level) => DiffType.New -> order
    }
    OrderBookDiff(next.atTime, prev, next, changes)
  }
}
