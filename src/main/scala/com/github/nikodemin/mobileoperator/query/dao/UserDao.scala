package com.github.nikodemin.mobileoperator.query.dao

import java.time.LocalDate

import com.github.nikodemin.mobileoperator.query.dao.util.Implicits._
import com.github.nikodemin.mobileoperator.query.model.dto.UserQueryDto
import com.github.nikodemin.mobileoperator.query.model.entity.QueryEntities._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class UserDao(implicit db: Database, executionContext: ExecutionContext) {

  def getByQueryDto(userQueryDto: UserQueryDto): Future[Seq[(UserRow, Option[AccountRow])]] = {
    joinAccounts.filterOpt(userQueryDto.email)((e, email) => e._1.email === email)
      .filterOpt(userQueryDto.dateOfBirth)((e, dateOfBirth) => e._1.dateOfBirth === dateOfBirth)
      .filterOpt(userQueryDto.firstName)((e, firstName) => e._1.firstName === firstName)
      .filterOpt(userQueryDto.lastName)((e, lastName) => e._1.lastName === lastName)
      .result
  }

  def getByEmail(email: String): Future[Seq[(UserRow, Option[AccountRow])]] =
    users.filter(_.email === email).joinLeft(accounts).on(_.email === _.userEmail).result

  def isUserExists(email: String): Future[Boolean] = users.filter(_.email === email).exists.result

  def changeOrAddUser(email: String, firstName: Option[String], lastName: Option[String], dateOfBirth: Option[LocalDate]): Future[Boolean] = {
    val user: Future[Option[UserRow]] = users.filter(_.email === email).forUpdate.result.headOption
    user.flatMap {
      case Some(user) =>
        users.filter(_.email === email)
          .map(u => (u.firstName, u.lastName, u.dateOfBirth))
          .update((firstName.getOrElse(user.firstName), lastName.getOrElse(user.lastName),
            dateOfBirth.getOrElse(user.dateOfBirth)))
      case None => users += UserRow(email, firstName.orNull, lastName.orNull, dateOfBirth.orNull)
    }
  }

  private def joinAccounts = users joinLeft accounts on (_.email === _.userEmail)
}
