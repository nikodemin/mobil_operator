package com.github.nikodemin.mobileoperator.query.service

import com.github.nikodemin.mobileoperator.query.dao.AccountDao
import com.github.nikodemin.mobileoperator.query.model.dto.{AccountGetByLastTakeOffDate, AccountQueryDto, AccountResponseDto}
import com.github.nikodemin.mobileoperator.query.model.entity.QueryEntities

import scala.concurrent.{ExecutionContext, Future}

class AccountQueryService(accountDao: AccountDao)
                         (implicit executionContext: ExecutionContext) {

  import AccountQueryService.mapToAccountGetDto


  def getByQueryDto(query: AccountQueryDto): Future[Seq[AccountResponseDto]] = accountDao.getByQueryDto(query)

  def getByLastTakeOffDateBetween(dto: AccountGetByLastTakeOffDate): Future[Seq[AccountResponseDto]] =
    accountDao.getByLastTakeOffDateBetween(dto.start, dto.end)

  def getByBalanceLowerThat(balance: Long): Future[Seq[AccountResponseDto]] = accountDao.getByBalanceLowerThat(balance)
}

object AccountQueryService {
  implicit def mapToAccountGetDto(future: Future[Seq[QueryEntities.AccountRow]])
                                 (implicit executionContext: ExecutionContext): Future[Seq[AccountResponseDto]] =
    future.map(_.map(AccountResponseDto.fromRow))
}
