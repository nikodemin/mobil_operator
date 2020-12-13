package com.github.nikodemin.mobileoperator.query.handler

import java.time.LocalDateTime

import akka.Done
import akka.event.slf4j.SLF4JLogging
import com.github.nikodemin.mobileoperator.query.dao.AccountDao

import scala.concurrent.{ExecutionContext, Future}

final class AccountHandler(accountDao: AccountDao)
                          (implicit executionContext: ExecutionContext) extends SLF4JLogging {

  def deactivate(phoneNumber: String, retryCount: Int = 3): Future[Done] =
    accountDao.deactivate(phoneNumber: String).flatMap { res =>
      if (!res) {
        log.error(s"Deactivate event is not persisted. Retry count: $retryCount")

        if (retryCount > 0) deactivate(phoneNumber, retryCount - 1) else
          Future.failed(new IllegalStateException("Deactivate event is not persisted: retryCount is zero"))
      } else Future(Done)
    }

  def activate(phoneNumber: String, retryCount: Int = 3): Future[Done] =
    accountDao.activate(phoneNumber: String).flatMap { res =>
      if (!res) {
        log.error(s"Activate event is not persisted. Retry count: $retryCount")

        if (retryCount > 0) activate(phoneNumber, retryCount - 1) else
          Future.failed(new IllegalStateException("Activate event is not persisted: retryCount is zero"))
      } else Future(Done)
    }

  def setPricingPlan(phoneNumber: String, name: String, price: Int, retryCount: Int = 3): Future[Done] =
    accountDao.setPricingPlan(phoneNumber, name, price).flatMap { res =>
      if (!res) {
        log.error(s"SetPricingPlan event is not persisted. Retry count: $retryCount")

        if (retryCount > 0) setPricingPlan(phoneNumber, name, price, retryCount - 1) else
          Future.failed(new IllegalStateException("SetPricingPlan event is not persisted: retryCount is zero"))
      } else Future(Done)
    }

  def takeOff(phoneNumber: String, amount: Int, dateTime: LocalDateTime, retryCount: Int = 3): Future[Done] =
    accountDao.takeOff(phoneNumber, amount, dateTime).flatMap { res =>
      if (!res) {
        log.error(s"TakeOff event is not persisted. Retry count: $retryCount")

        if (retryCount > 0) takeOff(phoneNumber, amount, dateTime, retryCount - 1) else
          Future.failed(new IllegalStateException("TakeOff event is not persisted: retryCount is zero"))
      } else Future(Done)
    }

  def pay(phoneNumber: String, amount: Int, retryCount: Int = 3): Future[Done] =
    accountDao.pay(phoneNumber, amount).flatMap { res =>
      if (!res) {
        log.error(s"Pay event is not persisted. Retry count: $retryCount")

        if (retryCount > 0) pay(phoneNumber, amount, retryCount - 1) else
          Future.failed(new IllegalStateException("Pay event is not persisted: retryCount is zero"))
      } else Future(Done)
    }
}
