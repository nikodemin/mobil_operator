package com.github.nikodemin.mobileoperator.cmd.model.dto

import java.time.LocalDateTime

import akka.actor.typed.ActorRef
import com.github.nikodemin.mobileoperator.cmd.actor.AccountActor.{SetPricingPlan, State}

case class AccountResponseDto(phoneNumber: String, pricingPlanName: String, pricingPlan: Int, accountBalance: Long,
                              lastTakeOffDate: LocalDateTime, isActive: Boolean)

object AccountResponseDto {
  def fromState(state: State, phoneNumber: String) = AccountResponseDto(phoneNumber, state.pricingPlanName,
    state.pricingPlan, state.accountBalance, state.lastTakeOffDate, state.isActive)
}

case class SetPricingPlanDto(name: String, price: Int) {
  def toCommand(ref: ActorRef[State]) = SetPricingPlan(name, price, Some(ref))
}