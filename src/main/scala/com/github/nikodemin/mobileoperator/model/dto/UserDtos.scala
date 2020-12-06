package com.github.nikodemin.mobileoperator.model.dto

import java.time.LocalDate

case class AddUserDto(firstName: String, lastName: String, email: String, dateOfBirth: LocalDate)
