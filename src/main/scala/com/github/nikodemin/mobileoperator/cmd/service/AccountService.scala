package com.github.nikodemin.mobileoperator.cmd.service

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import com.github.nikodemin.mobileoperator.cmd.actor.AccountActor.State
import com.github.nikodemin.mobileoperator.cmd.actor.{AccountActor, UserActor}
import com.github.nikodemin.mobileoperator.cmd.model.dto.{AccountAddDto, AccountGetDto, SetPricingPlanDto}

import scala.concurrent.{ExecutionContext, Future}

class AccountService(sharding: ClusterSharding)
                    (implicit executionContext: ExecutionContext, timeout: Timeout) {
  def addAccount(addAccountDto: AccountAddDto) = Future.apply {
    val user = sharding.entityRefFor(UserActor.typeKey, UserActor.entityId(addAccountDto.email))
    user ! addAccountDto.toCommand
    true
  }

  def activateAccount(phoneNumber: String) = Future {
    val account = sharding.entityRefFor(AccountActor.typeKey, AccountActor.entityId(phoneNumber))
    account ! AccountActor.Activate
    true
  }

  def deactivateAccount(phoneNumber: String) = Future {
    val account = sharding.entityRefFor(AccountActor.typeKey, AccountActor.entityId(phoneNumber))
    account ! AccountActor.Deactivate
    true
  }

  def getByPhoneNumber(phoneNumber: String) = {
    val account = sharding.entityRefFor(AccountActor.typeKey, AccountActor.entityId(phoneNumber))
    account.ask((ref: ActorRef[State]) => AccountActor.Get(ref))
      .map(AccountGetDto.fromState(_, phoneNumber))
  }

  def pay(phoneNumber: String, amount: Int) = Future {
    val account = sharding.entityRefFor(AccountActor.typeKey, AccountActor.entityId(phoneNumber))
    account ! AccountActor.Payment(amount)
    true
  }

  def setPricingPlan(phoneNumber: String, pricingPlan: SetPricingPlanDto) = Future {
    val account = sharding.entityRefFor(AccountActor.typeKey, AccountActor.entityId(phoneNumber))
    account ! pricingPlan.toCommand
    true
  }
}
