package com.github.nikodemin.mobileoperator.service

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.Offset
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import com.github.nikodemin.mobileoperator.actor.UserActor
import com.github.nikodemin.mobileoperator.actor.UserActor.State
import com.github.nikodemin.mobileoperator.model.dto.{UserAddDto, UserChangeDto, UserGetDto}

import scala.concurrent.{ExecutionContext, Future}

class UserService(sharding: ClusterSharding, journal: CassandraReadJournal)
                 (implicit executionContext: ExecutionContext, timeout: Timeout, materializer: Materializer) {
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

  def changeUser(email: String, userChangeDto: UserChangeDto) = Future {
    val user = sharding.entityRefFor(UserActor.typeKey, UserActor.entityId(email))
    user ! UserActor.ChangeUserData(
      firstName = Some(userChangeDto.firstName),
      lastName = Some(userChangeDto.lastName),
      email = Some(userChangeDto.email),
      dateOfBirth = Some(userChangeDto.dateOfBirth)
    )
    true
  }

  def getAllUsersEvents = journal.eventsByTag(UserActor.tag, Offset.noOffset)
    .map(_.event.asInstanceOf[UserActor.Event])
    .runWith(Sink.collection)
}
