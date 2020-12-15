package com.github.nikodemin.mobileoperator.query.service

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import com.github.nikodemin.mobileoperator.query.dao.AccountDao
import com.github.nikodemin.mobileoperator.query.model.dto.{AccountGetByLastTakeOffDate, AccountQueryDto, AccountResponseDto}
import com.github.nikodemin.mobileoperator.query.model.entity.QueryEntities.AccountRow
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

import scala.concurrent.Future

class AccountQueryServiceTest extends AsyncWordSpecLike
  with Matchers
  with AsyncMockFactory {
  private val accountDaoMock = mock[AccountDao]
  private val accountQueryService = new AccountQueryService(accountDaoMock)
  private val accountRow = AccountRow("phoneNumber", "email", "pricingPlanName",
    200, 0L, LocalDateTime.now, isActive = true)

  "AccountQueryService" should {
    "get by query" in {
      val accountQueryDto = AccountQueryDto(None, None, None, None)
      (accountDaoMock.getByQueryDto _).expects(accountQueryDto).returning(Future(Seq(accountRow)))

      accountQueryService.getByQueryDto(accountQueryDto).map {
        _ should contain(AccountResponseDto.fromRow(accountRow))
      }
    }

    "get by last take off date between" in {
      val startDate = LocalDateTime.now()
      val endDate = startDate.plus(2L, ChronoUnit.DAYS)
      val dto = AccountGetByLastTakeOffDate(startDate, endDate)
      (accountDaoMock.getByLastTakeOffDateBetween _).expects(startDate, endDate).returning(Future(Seq(accountRow)))

      accountQueryService.getByLastTakeOffDateBetween(dto).map {
        _ should contain(AccountResponseDto.fromRow(accountRow))
      }
    }

    "get by balance lower that" in {
      val balance = 200
      (accountDaoMock.getByBalanceLowerThat _).expects(balance).returning(Future(Seq(accountRow)))

      accountQueryService.getByBalanceLowerThat(balance).map {
        _ should contain(AccountResponseDto.fromRow(accountRow))
      }
    }
  }
}
