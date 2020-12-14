package com.github.nikodemin.mobileoperator.query.projection

import akka.Done
import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.Handler
import com.github.nikodemin.mobileoperator.cmd.actor.AccountActor
import com.github.nikodemin.mobileoperator.query.handler.AccountHandler

import scala.concurrent.Future

class AccountProjectionHandler(accountHandler: AccountHandler) extends Handler[EventEnvelope[AccountActor.Event]] {

  override def process(envelope: EventEnvelope[AccountActor.Event]): Future[Done] = envelope.event match {
    case AccountActor.PaymentReceived(phoneNumber, amount) => accountHandler.pay(phoneNumber, amount)
    case AccountActor.ChargedOff(phoneNumber, amount, dateTime) => accountHandler.takeOff(phoneNumber, amount, dateTime)
    case AccountActor.PricingPlanSet(phoneNumber, name, price) => accountHandler.setPricingPlan(phoneNumber, name, price)
    case AccountActor.Deactivated(phoneNumber) => accountHandler.deactivate(phoneNumber)
    case AccountActor.Activated(phoneNumber) => accountHandler.activate(phoneNumber)
  }
}
