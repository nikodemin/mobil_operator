package com.github.nikodemin.mobileoperator.service

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import com.github.nikodemin.mobileoperator.actor.UserActor
import com.github.nikodemin.mobileoperator.actor.UserActor.State
import com.github.nikodemin.mobileoperator.model.dto.{UserAddDto, UserGetDto}

import scala.concurrent.{ExecutionContext, Future}

class UserService(sharding: ClusterSharding)(implicit executionContext: ExecutionContext, timeout: Timeout) {
  def addUser(addUserDto: UserAddDto) = Future.apply {
    val user = sharding.entityRefFor(UserActor.typeKey, UserActor.entityId(addUserDto.email))
    user ! UserActor.ChangeUserData(
      firstName = Some(addUserDto.firstName),
      lastName = Some(addUserDto.lastName),
      email = Some(addUserDto.email),
      dateOfBirth = Some(addUserDto.dateOfBirth)
    )
    true
  }

  def getUserByEmail(email: String) = {
    val user = sharding.entityRefFor(UserActor.typeKey, UserActor.entityId(email))
    user.ask((ref: ActorRef[State]) => UserActor.Get(ref))
      .map(UserGetDto.fromState)
  }
}
