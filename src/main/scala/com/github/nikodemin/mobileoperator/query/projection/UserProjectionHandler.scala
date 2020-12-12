package com.github.nikodemin.mobileoperator.query.projection

import akka.Done
import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.Handler
import com.github.nikodemin.mobileoperator.cmd.actor.UserActor
import com.github.nikodemin.mobileoperator.query.handler.UserHandler

import scala.concurrent.Future

class UserProjectionHandler(userHandler: UserHandler) extends Handler[EventEnvelope[UserActor.Event]] {

  override def process(envelope: EventEnvelope[UserActor.Event]): Future[Done] = {
    envelope.event match {
      case UserActor.AccountAdded(phoneNumber, pricingPlanName, pricingPlan) => userHandler.addPhoneNumber(phoneNumber)
      case UserActor.UserDataChanged(firstName, lastName, dateOfBirth) => userHandler.changeUserData(firstName, lastName, dateOfBirth)
    }
  }
}
