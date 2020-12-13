package com.github.nikodemin.mobileoperator.cmd.actor

import java.time.LocalDate

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.delivery.ShardingProducerController.EntityId
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import akka.util.Timeout
import com.github.nikodemin.mobileoperator.common.serialization.CborSerializable

import scala.concurrent.duration._

object UserActor {

  sealed trait Command extends CborSerializable

  case class AddAccount(phoneNumber: String, pricingPlanName: String, pricingPlan: Int, replyTo: ActorRef[State]) extends Command

  case class ChangeUserData(firstName: Option[String], lastName: Option[String],
                            dateOfBirth: Option[LocalDate], replyTo: ActorRef[State]) extends Command


  sealed trait Event extends CborSerializable

  case class AccountAdded(email: String, phoneNumber: String, pricingPlanName: String, pricingPlan: Int) extends Event

  case class UserDataChanged(email: String, firstName: Option[String], lastName: Option[String],
                             dateOfBirth: Option[LocalDate]) extends Event


  case class State(firstName: String, lastName: String, dateOfBirth: LocalDate,
                   phoneNumbers: List[String]) extends CborSerializable


  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("UserActor")

  val tag = "User Actor"

  def entityId(email: String): String = email

  def apply(sharding: ClusterSharding, entityId: EntityId): Behavior[Command] = Behaviors.setup { ctx =>

    val commandHandler: (State, Command) => Effect[Event, State] = (_, cmd) => {

      cmd match {
        case AddAccount(phoneNumber, pricingPlanName, pricingPlan, replyTo) =>
          Effect.persist(AccountAdded(entityId, phoneNumber, pricingPlanName, pricingPlan))
            .thenRun(replyTo ! _)

        case ChangeUserData(firstName, lastName, dateOfBirth, replyTo) =>
          Effect.persist(UserDataChanged(entityId, firstName, lastName, dateOfBirth))
            .thenRun(replyTo ! _)
      }
    }

    val eventHandler: (State, Event) => State = (state, event) => {

      event match {
        case AccountAdded(email, phoneNumber, pricingPlanName, pricingPlan) =>
          val account = sharding.entityRefFor(AccountActor.typeKey, AccountActor.entityId(phoneNumber))
          implicit val timeout: Timeout = Timeout(10.seconds)

          ctx.ask(account, (ref: ActorRef[AccountActor.State]) => AccountActor.SetPricingPlan(pricingPlanName,
            pricingPlan, ref))(_)

          state.copy(phoneNumbers = state.phoneNumbers.prepended(phoneNumber))

        case UserDataChanged(email, firstName, lastName, dateOfBirth) =>
          state.copy(
            firstName = firstName.getOrElse(state.firstName),
            lastName = lastName.getOrElse(state.lastName),
            dateOfBirth = dateOfBirth.getOrElse(state.dateOfBirth)
          )
      }
    }

    EventSourcedBehavior(
      PersistenceId(typeKey.name, entityId),
      State(firstName = "", lastName = "", dateOfBirth = null, List()),
      commandHandler,
      eventHandler
    ).withRetention(RetentionCriteria.snapshotEvery(10, 3).withDeleteEventsOnSnapshot)
      .withTagger(_ => Set(tag))
      .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1))
  }
}
