package com.github.nikodemin.mobileoperator.service

import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.github.nikodemin.mobileoperator.actor.UserActor
import com.github.nikodemin.mobileoperator.model.dto.AccountAddDto

import scala.concurrent.{ExecutionContext, Future}

class AccountService(sharding: ClusterSharding)(implicit executionContext: ExecutionContext) {
  def addAccount(addAccountDto: AccountAddDto) = Future.apply {
    val user = sharding.entityRefFor(UserActor.typeKey, UserActor.entityId(addAccountDto.email))
    user ! UserActor.AddAccount(addAccountDto.phoneNumber, addAccountDto.pricingPlanName, addAccountDto.pricingPlan)
    true
  }
}
