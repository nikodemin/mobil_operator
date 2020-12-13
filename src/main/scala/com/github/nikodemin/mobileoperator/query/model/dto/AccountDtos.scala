package com.github.nikodemin.mobileoperator.query.model.dto

import java.time.LocalDateTime

import com.github.nikodemin.mobileoperator.query.model.entity.QueryEntities.AccountRow

case class AccountResponseDto(phoneNumber: String, pricingPlanName: String, pricingPlan: Int, accountBalance: Long,
                              lastTakeOffDate: LocalDateTime, isActive: Boolean)

object AccountResponseDto {
  def fromRow(row: AccountRow) = AccountResponseDto(row.phoneNumber, row.pricingPlanName, row.pricingPlan, row.accountBalance,
    row.lastTakeOffDate, row.isActive)
}

case class AccountQueryDto(phoneNumber: Option[String], pricingPlanName: Option[String], pricingPlan: Option[Int],
                           isActive: Option[Boolean])

case class AccountGetByLastTakeOffDate(start: LocalDateTime, end: LocalDateTime)

