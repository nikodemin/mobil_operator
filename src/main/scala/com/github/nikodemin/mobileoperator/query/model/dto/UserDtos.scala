package com.github.nikodemin.mobileoperator.query.model.dto

import java.time.LocalDate

import com.github.nikodemin.mobileoperator.query.model.entity.QueryEntities.{AccountRow, UserRow}

case class UserQueryDto(email: Option[String], firstName: Option[String], lastName: Option[String],
                        dateOfBirth: Option[LocalDate])

case class UserResponseDto(email: String, firstName: String, lastName: String, dateOfBirth: LocalDate, accounts: Seq[AccountResponseDto])

object UserResponseDto {
  def fromEntry(e: (UserRow, Seq[AccountRow])) = e match {
    case (user, accounts) => UserResponseDto(user.email, user.firstName, user.lastName, user.dateOfBirth,
      accounts.map(AccountResponseDto.fromRow))
  }
}
