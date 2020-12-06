package com.github.nikodemin.mobileoperator.model.dto

import java.time.LocalDateTime

import com.github.nikodemin.mobileoperator.actor.AccountActor.{SetPricingPlan, State}
import com.github.nikodemin.mobileoperator.actor.UserActor.AddAccount

case class AccountAddDto(email: String, phoneNumber: String, pricingPlanName: String, pricingPlan: Int) {
  def toCommand = AddAccount(phoneNumber, pricingPlanName, pricingPlan)
}

case class AccountGetDto(phoneNumber: String, pricingPlanName: String, pricingPlan: Int, accountBalance: Long,
                         lastTakeOffDate: LocalDateTime, isActive: Boolean)

object AccountGetDto {
  def fromState(state: State, phoneNumber: String) = AccountGetDto(phoneNumber, state.pricingPlanName,
    state.pricingPlan, state.accountBalance, state.lastTakeOffDate, state.isActive)
}

case class SetPricingPlanDto(name: String, price: Int) {
  def toCommand = SetPricingPlan(name, price)
}