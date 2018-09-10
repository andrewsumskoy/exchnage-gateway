package com.sumskoy.exchange.gateway.server
package provider

import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}

/**
  * OrderBookProvider is Akka Extension for order book subscribe functionality
  */
object OrderBookProvider extends ExtensionId[OrderBookProviderImpl] with ExtensionIdProvider {
  override def lookup = OrderBookProvider

  override def createExtension(system: ExtendedActorSystem) = new OrderBookProviderImpl(system)

  override def get(system: ActorSystem): OrderBookProviderImpl = super.get(system)
}



class OrderBookProviderImpl(system: ActorSystem) extends Extension {

  private val manager = system.actorOf(OrderBookSubscribeManagerActor.props, "orderbook_subscribe_manager")

  def subscribe(exchange: String, pair: String, receiver: ActorRef): Unit = {
    manager ! OrderBookSubscribeManagerActor.Subscribe(exchange, pair, receiver)
  }

  def unsubscribe(exchange: String, pair: String, receiver: ActorRef): Unit = {
    manager ! OrderBookSubscribeManagerActor.UnSubscribe(exchange, pair, receiver)
  }
}
