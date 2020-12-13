package com.github.nikodemin.mobileoperator.cmd.model.dto

import java.time.LocalDate

import akka.actor.typed.ActorRef
import com.github.nikodemin.mobileoperator.cmd.actor.UserActor
import com.github.nikodemin.mobileoperator.cmd.actor.UserActor.{AddAccount, State}

case class UserAddDto(firstName: String, lastName: String, email: String, dateOfBirth: LocalDate)

case class UserResponseDto(firstName: String, lastName: String, email: String, dateOfBirth: LocalDate,
                           phoneNumbers: List[String])

object UserResponseDto {
  def fromState(state: State, email: String) = UserResponseDto(state.firstName, state.lastName, email, state.dateOfBirth,
    state.phoneNumbers)
}

case class UserChangeDto(firstName: Option[String], lastName: Option[String], dateOfBirth: Option[LocalDate])

case class AccountAddDto(email: String, phoneNumber: String, pricingPlanName: String, pricingPlan: Int) {
  def toCommand(ref: ActorRef[UserActor.State]) = AddAccount(phoneNumber, pricingPlanName, pricingPlan, ref)
}
