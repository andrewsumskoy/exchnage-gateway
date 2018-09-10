package com.sumskoy.exchange.gateway.server
package provider

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props, Terminated}
import akka.stream.ActorMaterializer
import com.sumskoy.exchange.gateway.server.domain.OrderBookDiff
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.{Exchange, ExchangeFactory}

import scala.concurrent.duration.Duration
import scala.util.Try

/**
  * Order book subscribe manager provide full control of exchange connections and order book update stream.
  * Exchange configs look at exchange-gateway.server.exchanges.${name}.${class|interval}
  */
class OrderBookSubscribeManagerActor extends Actor with ActorLogging {

  import OrderBookSubscribeManagerActor._

  implicit val system = context.system
  implicit val materializer = ActorMaterializer()

  val settings = context.system.settings.config.getConfig("exchange-gateway.server.exchanges")

  var exchanges = Map.empty[String, Exchange]
  var subscribes = Map.empty[(String, String), List[ActorRef]]
  var subscribeStreams = Map.empty[(String, String), Cancellable]

  override def receive: Receive = {
    case RegisterStream(e, p, c) => subscribeStreams += (e -> p) -> c
    case Subscribe(ex, p, ref) => Try(subscribe(ex, p, ref)).failed.foreach(e => log.error(e, s"Can not subscribe for $ex:$p"))
    case UnSubscribe(ex, p, ref) => unsubscribe(ex, p, ref)
    case OrderBook(e, p, o) => subscribes.getOrElse(e -> p, List.empty).foreach(_ ! (e, p, o))
    case Terminated(ref) => unsubcribeAll(ref)
  }

  def unsubcribeAll(ref: ActorRef): Unit = {
    subscribes.filter(_._2.contains(ref)).foreach { case ((ex, pr), _) =>
      unsubscribe(ex, pr, ref)
    }
  }

  def subscribe(exchange: String, pair: String, ref: ActorRef): Unit = {
    context.watch(ref)
    val ex: Exchange = exchanges.get(exchange) match {
      case Some(e) => e
      case _ =>
        val clazz = settings.getString(exchange + ".class")
        val e = ExchangeFactory.INSTANCE.createExchange(clazz)
        exchanges += exchange -> e
        e
    }
    subscribes.get(exchange -> pair) match {
      case Some(l) => subscribes += (exchange -> pair) -> (l :+ ref)
      case _ =>
        log.debug(s"Subscribe for $exchange:$pair")
        val interval = settings.getDuration(exchange + ".interval")
        OrderBookStream.diffSate(OrderBookStream.exchange(ex, new CurrencyPair(pair), Duration.fromNanos(interval.toNanos))).mapMaterializedValue { c =>
          self ! RegisterStream(exchange, pair, c)
        }.runForeach { o =>
          self ! OrderBook(exchange, pair, o)
        }
        subscribes += (exchange -> pair) -> List(ref)
    }
  }

  def unsubscribe(exchange: String, pair: String, ref: ActorRef): Unit = {
    val l = subscribes.getOrElse(exchange -> pair, List.empty).filter(_ != ref)
    if (l.isEmpty) {
      subscribes = subscribes.filter { case (k, _) =>
        k != (exchange -> pair)
      }
      subscribeStreams.get(exchange -> pair).forall(_.cancel())
      log.debug(s"Un subscribe for $exchange:$pair")
      if (!subscribes.exists(el => el._1._1 == exchange)) {
        exchanges = exchanges.filter(_._1 != exchange)
      }
    } else {
      subscribes += (exchange -> pair) -> l
    }
  }

  override def postStop(): Unit = {
    super.postStop()
    subscribeStreams.foreach(_._2.cancel())
  }
}

object OrderBookSubscribeManagerActor {

  def props = Props(new OrderBookSubscribeManagerActor())

  final case class RegisterStream(exchange: String, pair: String, c: Cancellable)
  final case class OrderBook(exchange: String, pair: String, o: OrderBookDiff)
  final case class Subscribe(exchange: String, pair: String, ref: ActorRef)
  final case class UnSubscribe(exchange: String, pair: String, ref: ActorRef)

}
