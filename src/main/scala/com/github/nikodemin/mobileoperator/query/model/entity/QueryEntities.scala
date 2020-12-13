package com.github.nikodemin.mobileoperator.query.model.entity

import java.time.{LocalDate, LocalDateTime}

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

object QueryEntities {

  case class UserRow(email: String, firstName: String, lastName: String, dateOfBirth: LocalDate)

  class User(tag: Tag) extends Table[UserRow](tag, "users") {
    val email = column[String]("email", O.PrimaryKey)
    val firstName = column[String]("first_name")
    val lastName = column[String]("last_name")
    val dateOfBirth = column[LocalDate]("date_of_birth")

    override def * = (email, firstName, lastName, dateOfBirth).mapTo[UserRow]
  }

  val users = TableQuery[User]


  case class AccountRow(phoneNumber: String, userEmail: String, pricingPlanName: String, pricingPlan: Int,
                        accountBalance: Long, lastTakeOffDate: LocalDateTime, isActive: Boolean)

  class Account(tag: Tag) extends Table[AccountRow](tag, "accounts") {
    val phoneNumber = column[String]("phone_number", O.PrimaryKey)
    val userEmail = column[String]("user_email")
    val pricingPlanName = column[String]("pricing_plan_name")
    val pricingPlan = column[Int]("pricing_plan")
    val accountBalance = column[Long]("account_balance")
    val lastTakeOffDate = column[LocalDateTime]("last_take_off_date")
    val isActive = column[Boolean]("is_active")

    val fk = foreignKey("user_FK", userEmail, users)(_.email, onDelete = ForeignKeyAction.Cascade)

    override def * = (phoneNumber, userEmail, pricingPlanName, pricingPlan,
      accountBalance, lastTakeOffDate, isActive).mapTo[AccountRow]
  }

  val accounts = TableQuery[Account]
}