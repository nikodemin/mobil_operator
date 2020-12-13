package com.github.nikodemin.mobileoperator.query.dao

import java.time.LocalDateTime

import com.github.nikodemin.mobileoperator.query.dao.util.Implicits._
import com.github.nikodemin.mobileoperator.query.model.entity.QueryEntities._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class AccountDao(implicit db: Database, executionContext: ExecutionContext) {
  def pay(phoneNumber: String, amount: Int): Future[Boolean] = getAccount(phoneNumber)
    .map(a => a.accountBalance)
    .update(amount)

  def takeOff(phoneNumber: String, amount: Int, dateTime: LocalDateTime): Future[Boolean] = getAccount(phoneNumber)
    .map(a => (a.accountBalance, a.lastTakeOffDate))
    .update((amount, dateTime))

  def setPricingPlan(phoneNumber: String, name: String, price: Int): Future[Boolean] = getAccount(phoneNumber)
    .map(a => (a.pricingPlanName, a.pricingPlan))
    .update((name, price))

  def activate(phoneNumber: String): Future[Boolean] = getAccount(phoneNumber)
    .map(_.isActive)
    .update(true)

  def deactivate(phoneNumber: String): Future[Boolean] = getAccount(phoneNumber)
    .map(_.isActive)
    .update(false)

  def addAccount(email: String, phoneNumber: String): Future[Boolean] =
    accounts += AccountRow(phoneNumber, email, "", 0, 0L, LocalDateTime.now(), isActive = false)


  private def getAccount(phoneNumber: String): Query[Account, AccountRow, Seq] =
    accounts.filter(_.phoneNumber === phoneNumber)
}
