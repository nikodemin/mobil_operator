package com.github.nikodemin.mobileoperator.service

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import com.github.nikodemin.mobileoperator.actor.UserActor
import com.github.nikodemin.mobileoperator.model.dto.AddUserDto

import scala.concurrent.{ExecutionContext, Future}

class UserService(sharding: ClusterSharding)(implicit executionContext: ExecutionContext) {
  def addUser(addUserDto: AddUserDto) = Future.apply {
    sharding.init(Entity(UserActor.typeKey) { _ =>
      UserActor(addUserDto.firstName, addUserDto.lastName, addUserDto.email, addUserDto.dateOfBirth, sharding,
        isQuery = false)
    })
    sharding.init(Entity(UserActor.typeKey) { _ =>
      UserActor(addUserDto.firstName, addUserDto.lastName, addUserDto.email, addUserDto.dateOfBirth, sharding,
        isQuery = true)
    })
    true
  }
}
