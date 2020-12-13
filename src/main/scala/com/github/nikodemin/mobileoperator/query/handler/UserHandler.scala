package com.github.nikodemin.mobileoperator.query.handler

import java.time.LocalDate

import akka.Done
import akka.event.slf4j.SLF4JLogging
import com.github.nikodemin.mobileoperator.query.dao.{AccountDao, UserDao}

import scala.concurrent.{ExecutionContext, Future}

final class UserHandler(userDao: UserDao, accountDao: AccountDao)
                       (implicit executionContext: ExecutionContext) extends SLF4JLogging {

  def addPhoneNumber(email: String, phoneNumber: String, retryCount: Int = 3): Future[Done] =
    accountDao.addAccount(email, phoneNumber).flatMap { res =>
      if (!res) {
        log.error(s"AddPhoneNumber event is not persisted. Retry count: $retryCount")

        if (retryCount > 0) addPhoneNumber(email, phoneNumber, retryCount - 1) else
          Future.failed(new IllegalStateException("AddPhoneNumber event is not persisted: retryCount is zero"))
      } else Future(Done)
    }

  def changeUserData(email: String, firstName: Option[String], lastName: Option[String],
                     dateOfBirth: Option[LocalDate], retryCount: Int = 3): Future[Done] =
    userDao.changeOrAddUser(email, firstName, lastName, dateOfBirth).flatMap { res =>
      if (!res) {
        log.error(s"ChangeUserData event is not persisted. Retry count: $retryCount")

        if (retryCount > 0) changeUserData(email, firstName, lastName, dateOfBirth, retryCount - 1) else
          Future.failed(new IllegalStateException("ChangeUserData event is not persisted: retryCount is zero"))
      } else Future(Done)
    }
}
