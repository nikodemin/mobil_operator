package com.github.nikodemin.mobileoperator.query.model.dto

import java.time.LocalDate

case class UserQueryDto(email: Option[String], firstName: Option[String], lastName: Option[String],
                        dateOfBirth: Option[LocalDate])

case class UserGetDto(email: String, firstName: String, lastName: String, dateOfBirth: LocalDate)
