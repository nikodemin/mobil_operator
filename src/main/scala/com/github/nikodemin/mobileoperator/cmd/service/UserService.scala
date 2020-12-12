package com.github.nikodemin.mobileoperator.cmd.service

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import com.github.nikodemin.mobileoperator.cmd.actor.UserActor
import com.github.nikodemin.mobileoperator.cmd.actor.UserActor.State
import com.github.nikodemin.mobileoperator.cmd.model.dto.{UserAddDto, UserChangeDto, UserGetDto}

import scala.concurrent.{ExecutionContext, Future}

class UserService(sharding: ClusterSharding)
                 (implicit executionContext: ExecutionContext, timeout: Timeout) {
  def addUser(addUserDto: UserAddDto) = Future.apply {
    val user = sharding.entityRefFor(UserActor.typeKey, UserActor.entityId(addUserDto.email))
    user ! UserActor.ChangeUserData(
      firstName = Some(addUserDto.firstName),
      lastName = Some(addUserDto.lastName),
      dateOfBirth = Some(addUserDto.dateOfBirth)
    )
    true
  }

  def getUserByEmail(email: String) = {
    val user = sharding.entityRefFor(UserActor.typeKey, UserActor.entityId(email))
    user.ask((ref: ActorRef[State]) => UserActor.Get(ref))
      .map(UserGetDto.fromState(_, email))
  }

  def changeUser(email: String, userChangeDto: UserChangeDto) = Future {
    val user = sharding.entityRefFor(UserActor.typeKey, UserActor.entityId(email))
    user ! UserActor.ChangeUserData(
      firstName = userChangeDto.firstName,
      lastName = userChangeDto.lastName,
      dateOfBirth = userChangeDto.dateOfBirth
    )
    true
  }
}
