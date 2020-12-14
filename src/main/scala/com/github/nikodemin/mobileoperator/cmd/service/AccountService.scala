package com.github.nikodemin.mobileoperator.cmd.service

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import com.github.nikodemin.mobileoperator.cmd.actor.AccountActor
import com.github.nikodemin.mobileoperator.cmd.actor.AccountActor.{Activate, Deactivate, Payment, State}
import com.github.nikodemin.mobileoperator.cmd.model.dto.{AccountResponseDto, SetPricingPlanDto}

import scala.concurrent.{ExecutionContext, Future}

class AccountService(sharding: ClusterSharding)
                    (implicit executionContext: ExecutionContext, timeout: Timeout) {

  def activateAccount(phoneNumber: String): Future[AccountResponseDto] = {
    val account = sharding.entityRefFor(AccountActor.typeKey, AccountActor.entityId(phoneNumber))

    account.ask((ref: ActorRef[State]) => Activate(Some(ref))).map(AccountResponseDto.fromState(_, phoneNumber))
  }

  def deactivateAccount(phoneNumber: String): Future[AccountResponseDto] = {
    val account = sharding.entityRefFor(AccountActor.typeKey, AccountActor.entityId(phoneNumber))

    account.ask((ref: ActorRef[State]) => Deactivate(Some(ref))).map(AccountResponseDto.fromState(_, phoneNumber))
  }

  def pay(phoneNumber: String, amount: Int): Future[AccountResponseDto] = {
    val account = sharding.entityRefFor(AccountActor.typeKey, AccountActor.entityId(phoneNumber))

    account.ask((ref: ActorRef[State]) => Payment(amount, Some(ref))).map(AccountResponseDto.fromState(_, phoneNumber))
  }

  def setPricingPlan(phoneNumber: String, pricingPlan: SetPricingPlanDto): Future[AccountResponseDto] = {
    val account = sharding.entityRefFor(AccountActor.typeKey, AccountActor.entityId(phoneNumber))

    account.ask((ref: ActorRef[State]) => pricingPlan.toCommand(ref)).map(AccountResponseDto.fromState(_, phoneNumber))
  }
}
