package com.github.nikodemin.mobileoperator.cmd.service

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import com.github.nikodemin.mobileoperator.cmd.actor.UserActor
import com.github.nikodemin.mobileoperator.cmd.actor.UserActor.State
import com.github.nikodemin.mobileoperator.cmd.model.dto.{AccountAddDto, UserAddDto, UserChangeDto, UserResponseDto}

import scala.concurrent.{ExecutionContext, Future}

class UserService(sharding: ClusterSharding)
                 (implicit executionContext: ExecutionContext, timeout: Timeout) {
  def addUser(addUserDto: UserAddDto): Future[UserResponseDto] = {
    val user = sharding.entityRefFor(UserActor.typeKey, UserActor.entityId(addUserDto.email))

    user.ask((ref: ActorRef[State]) => UserActor.ChangeUserData(
      firstName = Some(addUserDto.firstName),
      lastName = Some(addUserDto.lastName),
      dateOfBirth = Some(addUserDto.dateOfBirth),
      ref)).map(UserResponseDto.fromState(_, addUserDto.email))
  }

  def addAccount(addAccountDto: AccountAddDto): Future[UserResponseDto] = {
    val user = sharding.entityRefFor(UserActor.typeKey, UserActor.entityId(addAccountDto.email))

    user.ask((ref: ActorRef[UserActor.State]) => addAccountDto.toCommand(ref)).map(UserResponseDto.fromState(_, addAccountDto.email))
  }

  def changeUser(email: String, userChangeDto: UserChangeDto): Future[UserResponseDto] = {
    val user = sharding.entityRefFor(UserActor.typeKey, UserActor.entityId(email))

    user.ask((ref: ActorRef[State]) => UserActor.ChangeUserData(
      firstName = userChangeDto.firstName,
      lastName = userChangeDto.lastName,
      dateOfBirth = userChangeDto.dateOfBirth,
      ref)).map(UserResponseDto.fromState(_, email))
  }
}
