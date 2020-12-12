package com.github.nikodemin.mobileoperator.query.handler

import java.time.LocalDate

import akka.Done
import akka.event.slf4j.SLF4JLogging

import scala.concurrent.{ExecutionContext, Future}

class UserHandler(implicit executionContext: ExecutionContext) extends SLF4JLogging {

  def addPhoneNumber(phoneNumber: String): Future[Done] = Future {
    log.info(s"addPhoneNUmber: $phoneNumber")
    println(s"addPhoneNUmber: $phoneNumber")

    Done
  }

  def changeUserData(firstName: Option[String], lastName: Option[String], dateOfBirth: Option[LocalDate]): Future[Done] = Future {
    log.info(s"changeUserData firstName: $firstName, lastName: $lastName, dateOfBirth: $dateOfBirth")
    println(s"changeUserData firstName: $firstName, lastName: $lastName, dateOfBirth: $dateOfBirth")
    Done
  }
}
