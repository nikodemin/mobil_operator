package com.github.nikodemin.mobileoperator.model.dto

import java.time.LocalDate

import com.github.nikodemin.mobileoperator.actor.UserActor.State

case class UserAddDto(firstName: String, lastName: String, email: String, dateOfBirth: LocalDate)

case class UserGetDto(firstName: String, lastName: String, email: String, dateOfBirth: LocalDate)

object UserGetDto {
  def fromState(state: State) = UserGetDto(state.firstName, state.lastName, state.email, state.dateOfBirth)
}
