package com.github.nikodemin.mobileoperator.query.service

import com.github.nikodemin.mobileoperator.query.dao.UserDao
import com.github.nikodemin.mobileoperator.query.model.dto.{UserQueryDto, UserResponseDto}
import com.github.nikodemin.mobileoperator.query.model.entity.QueryEntities.{AccountRow, UserRow}

import scala.collection.MapView
import scala.concurrent.{ExecutionContext, Future}

class UserQueryService(userDao: UserDao)
                      (implicit executionContext: ExecutionContext) {

  def getByUserQuery(query: UserQueryDto): Future[Seq[UserResponseDto]] = userDao.getByQueryDto(query)
    .map(userAccountSeqToUserGetDtoSeq)

  def getByEmail(email: String): Future[Option[UserResponseDto]] = userDao.getByEmail(email)
    .map(userAccountSeqToUserGetDtoSeq)
    .map(_.headOption)

  private def userAccountSeqToUserGetDtoSeq(entry: Seq[(UserRow, Option[AccountRow])]): Seq[UserResponseDto] = {
    val userAccountsOptionMap: Map[UserRow, Seq[Option[AccountRow]]] = entry.groupMap(_._1)(_._2)
    val userAccountsMap: MapView[UserRow, Seq[AccountRow]] =
      userAccountsOptionMap.view.mapValues(_.filter(_.isDefined).map(_.get))

    userAccountsMap.map(UserResponseDto.fromEntry).toSeq
  }
}
