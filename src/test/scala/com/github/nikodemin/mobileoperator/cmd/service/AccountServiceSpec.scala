package com.github.nikodemin.mobileoperator.cmd.service

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.cluster.sharding.typed.testkit.scaladsl.TestEntityRef
import akka.util.Timeout
import com.github.nikodemin.mobileoperator.cmd.actor.AccountActor
import com.github.nikodemin.mobileoperator.cmd.model.dto.SetPricingPlanDto
import com.github.nikodemin.mobileoperator.util.ClusterShardingMock
import com.typesafe.config.ConfigFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class AccountServiceSpec extends AnyWordSpecLike
  with Matchers
  with ScalaCheckPropertyChecks
  with MockFactory {
  private val testKit = ActorTestKit("AccountServiceSpec", ConfigFactory.load)
  private val probe = testKit.createTestProbe[AccountActor.Command]
  private val shardingMock = mock[ClusterShardingMock]
  private implicit val timeout: Timeout = Timeout(1.second)
  private val accountService = new AccountService(shardingMock)
  private val entityRef = TestEntityRef(AccountActor.typeKey, "", probe.ref)

  "Account service" should {
    "activate account" in {
      forAll("phoneNumber") { phoneNumber: String =>
        (shardingMock.entityRefFor(_: EntityTypeKey[AccountActor.Command], _: String))
          .expects(AccountActor.typeKey, AccountActor.entityId(phoneNumber)).returning(entityRef)

        accountService.activateAccount(phoneNumber)

        probe.expectMessageType[AccountActor.Activate]
      }
    }

    "deactivate account" in {
      forAll("phoneNumber") { phoneNumber: String =>
        (shardingMock.entityRefFor(_: EntityTypeKey[AccountActor.Command], _: String))
          .expects(AccountActor.typeKey, AccountActor.entityId(phoneNumber)).returning(entityRef)

        accountService.deactivateAccount(phoneNumber)

        probe.expectMessageType[AccountActor.Deactivate]
      }
    }

    "pay" in {
      forAll("phoneNumber", "amount") { (phoneNumber: String, amount: Int) =>
        (shardingMock.entityRefFor(_: EntityTypeKey[AccountActor.Command], _: String))
          .expects(AccountActor.typeKey, AccountActor.entityId(phoneNumber)).returning(entityRef)

        accountService.pay(phoneNumber, amount)

        val message = probe.receiveMessage
        assert(message.isInstanceOf[AccountActor.Payment])
        val actual = message.asInstanceOf[AccountActor.Payment]
        actual.amount should ===(amount)
      }
    }

    "set pricing plan" in {
      forAll("phoneNumber", "pricingPlanName", "pricingPlan") {
        (phoneNumber: String, pricingPlanName: String, pricingPlan: Int) =>
          val setPricingPlanDto = SetPricingPlanDto(pricingPlanName, pricingPlan)

          (shardingMock.entityRefFor(_: EntityTypeKey[AccountActor.Command], _: String))
            .expects(AccountActor.typeKey, AccountActor.entityId(phoneNumber)).returning(entityRef)

          accountService.setPricingPlan(phoneNumber, setPricingPlanDto)

          val message = probe.receiveMessage
          assert(message.isInstanceOf[AccountActor.SetPricingPlan])
          val actual = message.asInstanceOf[AccountActor.SetPricingPlan]
          actual.name should ===(pricingPlanName)
          actual.price should ===(pricingPlan)
      }
    }
  }
}
