package com.github.nikodemin.mobileoperator.model.dto

import java.time.LocalDate

import com.github.nikodemin.mobileoperator.actor.UserActor.State

case class UserAddDto(firstName: String, lastName: String, email: String, dateOfBirth: LocalDate)

case class UserGetDto(firstName: String, lastName: String, email: String, dateOfBirth: LocalDate,
                      phoneNumbers: List[String])

object UserGetDto {
  def fromState(state: State, email: String) = UserGetDto(state.firstName, state.lastName, email, state.dateOfBirth,
    state.phoneNumbers)
}

case class UserChangeDto(firstName: Option[String], lastName: Option[String], dateOfBirth: Option[LocalDate])