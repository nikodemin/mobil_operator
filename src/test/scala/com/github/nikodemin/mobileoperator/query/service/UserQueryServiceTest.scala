package com.github.nikodemin.mobileoperator.query.service

import java.time.{LocalDate, LocalDateTime}

import com.github.nikodemin.mobileoperator.query.dao.UserDao
import com.github.nikodemin.mobileoperator.query.model.dto.{AccountResponseDto, UserQueryDto, UserResponseDto}
import com.github.nikodemin.mobileoperator.query.model.entity.QueryEntities.{AccountRow, UserRow}
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

import scala.concurrent.Future

class UserQueryServiceTest extends AsyncWordSpecLike
  with Matchers
  with AsyncMockFactory {
  private val userDaoMock = mock[UserDao]
  private val userQueryService = new UserQueryService(userDaoMock)

  private val email = "email"
  private val firstName = "firstName"
  private val lastName = "lastName"
  private val dateOfBirth = LocalDate.now()
  private val userRow = UserRow(email, firstName, lastName, dateOfBirth)
  private val phoneNumber = "phoneNumber"
  private val pricingPlanName = "pricingPlanName"
  private val pricingPlan = 100
  private val accountBalance = 200L
  private val lastTakeOffDate = LocalDateTime.now
  private val isActive = true
  private val accountRow = AccountRow(phoneNumber, email, pricingPlanName, pricingPlan,
    accountBalance, lastTakeOffDate, isActive)

  "UserQueryService" should {
    "get by query" in {
      val userQueryDto = UserQueryDto(None, None, None, None)
      (userDaoMock.getByQueryDto _).expects(userQueryDto).returning(Future(Seq((userRow, Some(accountRow)))))

      userQueryService.getByUserQuery(userQueryDto).map {
        _ should ===(Seq(UserResponseDto(email, firstName, lastName, dateOfBirth,
          Seq(AccountResponseDto(phoneNumber, pricingPlanName, pricingPlan, accountBalance, lastTakeOffDate, isActive)))))
      }
    }

    "get by email" in {
      (userDaoMock.getByEmail _).expects(email).returning(Future(Seq((userRow, Some(accountRow)))))

      userQueryService.getByEmail(email).map {
        _ should ===(Some(UserResponseDto(email, firstName, lastName, dateOfBirth,
          Seq(AccountResponseDto(phoneNumber, pricingPlanName, pricingPlan, accountBalance, lastTakeOffDate, isActive)))))
      }
    }
  }
}
