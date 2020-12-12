package com.github.nikodemin.mobileoperator.cmd.service

import java.time.LocalDate

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.cluster.sharding.typed.testkit.scaladsl.TestEntityRef
import akka.util.Timeout
import com.github.nikodemin.mobileoperator.cmd.actor.UserActor
import com.github.nikodemin.mobileoperator.cmd.model.dto.{UserAddDto, UserChangeDto}
import com.github.nikodemin.mobileoperator.util.ClusterShardingMock
import com.typesafe.config.ConfigFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class UserServiceSpec extends AnyWordSpecLike
  with Matchers
  with ScalaCheckPropertyChecks
  with MockFactory {
  private val testKit = ActorTestKit("UserServiceSpec", ConfigFactory.load)
  private val probe = testKit.createTestProbe[UserActor.Command]
  private val shardingMock = mock[ClusterShardingMock]
  private implicit val timeout: Timeout = Timeout(1.second)
  private val userService = new UserService(shardingMock)

  "User service" should {
    "add user" in {
      forAll("email", "firstName", "lastName") {
        (email: String, firstName: String, lastName: String) =>
          val dateOfBirth = LocalDate.now
          val userAddDto = UserAddDto(firstName, lastName, email, dateOfBirth)
          val entityRef = TestEntityRef(UserActor.typeKey, email, probe.ref)

          (shardingMock.entityRefFor(_: EntityTypeKey[UserActor.Command], _: String))
            .expects(UserActor.typeKey, UserActor.entityId(email)).returning(entityRef)

          userService.addUser(userAddDto)
          val message: UserActor.Command = probe.receiveMessage

          assert(message.isInstanceOf[UserActor.ChangeUserData])
          val actual = message.asInstanceOf[UserActor.ChangeUserData]
          actual.dateOfBirth.get should ===(dateOfBirth)
          actual.firstName.get should ===(firstName)
          actual.lastName.get should ===(lastName)
      }
    }

    "get user by email" in {
      forAll("email") { email: String =>
        val entityRef = TestEntityRef(UserActor.typeKey, email, probe.ref)
        (shardingMock.entityRefFor(_: EntityTypeKey[UserActor.Command], _: String))
          .expects(UserActor.typeKey, UserActor.entityId(email)).returning(entityRef)

        userService.getUserByEmail(email)

        probe.expectMessageType[UserActor.Get]
      }
    }

    "change user" in {
      forAll("email", "firstName", "lastName") {
        (email: String, firstName: String, lastName: String) =>
          val dateOfBirth = LocalDate.now
          val userChangeDto = UserChangeDto(Some(firstName), Some(lastName), Some(dateOfBirth))

          val entityRef = TestEntityRef(UserActor.typeKey, email, probe.ref)
          (shardingMock.entityRefFor(_: EntityTypeKey[UserActor.Command], _: String))
            .expects(UserActor.typeKey, UserActor.entityId(email)).returning(entityRef)

          userService.changeUser(email, userChangeDto)

          val message = probe.receiveMessage
          assert(message.isInstanceOf[UserActor.ChangeUserData])
          val actual = message.asInstanceOf[UserActor.ChangeUserData]
          actual.dateOfBirth.get should ===(dateOfBirth)
          actual.firstName.get should ===(firstName)
          actual.lastName.get should ===(lastName)

      }
    }
  }
}
