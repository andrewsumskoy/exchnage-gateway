package com.sumskoy.exchange.gateway.server
package domain

import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.dto.trade

object OrderSide extends Enumeration {
  val Bid = Value("Bid")
  val Ask = Value("Ask")
}

case class LimitOrder(pair: CurrencyPair, side: OrderSide.Value, price: BigDecimal, originalAmount: BigDecimal, remainingAmount: BigDecimal)

object LimitOrder {
  def apply(side: OrderSide.Value, o: trade.LimitOrder): LimitOrder =
    LimitOrder(
      o.getCurrencyPair,
      side,
      price = BigDecimal(o.getLimitPrice),
      originalAmount = BigDecimal(o.getOriginalAmount),
      remainingAmount = BigDecimal(o.getRemainingAmount)
    )

}
