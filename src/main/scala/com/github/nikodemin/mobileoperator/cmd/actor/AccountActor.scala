package com.github.nikodemin.mobileoperator.cmd.actor

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.delivery.ShardingProducerController.EntityId
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import com.github.nikodemin.mobileoperator.common.serialization.CborSerializable
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

object AccountActor {

  sealed trait Command extends CborSerializable

  case class Payment(amount: Int, replyTo: ActorRef[State]) extends Command

  private case class TakeOff(amount: Int) extends Command

  case class SetPricingPlan(name: String, price: Int, replyTo: ActorRef[State]) extends Command

  case class Deactivate(replyTo: ActorRef[State]) extends Command

  case class Activate(replyTo: ActorRef[State]) extends Command


  sealed trait Event extends CborSerializable

  case class PaymentReceived(phoneNumber: String, amount: Int) extends Event

  case class ChargedOff(phoneNumber: String, amount: Int, dateTime: LocalDateTime) extends Event

  case class PricingPlanSet(phoneNumber: String, name: String, price: Int) extends Event

  case class Deactivated(phoneNumber: String) extends Event

  case class Activated(phoneNumber: String) extends Event


  case class State(pricingPlanName: String, pricingPlan: Int, accountBalance: Long, lastTakeOffDate: LocalDateTime,
                   isActive: Boolean) extends CborSerializable

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("AccountActor")

  val tag = "Account actor"

  def entityId(phoneNumber: String) = phoneNumber

  def apply(entityId: EntityId): Behavior[Command] =
    Behaviors.setup { ctx =>
      Behaviors.withTimers { timerScheduler =>
        val chargePeriod = ConfigFactory.load().getLong("mobile-operator.charge-period")
        val timerKey = "User actor"

        def calculateResidue(startDateTime: LocalDateTime, oldPrice: Int) =
          (ChronoUnit.SECONDS.between(startDateTime, LocalDateTime.now()) * oldPrice / chargePeriod).toInt

        val commandHandler: (State, Command) => Effect[Event, State] = (state, cmd) => {

          cmd match {
            case Payment(amount, replyTo) => Effect.persist(PaymentReceived(entityId, amount))
              .thenRun(replyTo ! _)

            case TakeOff(amount) => Effect.persist(ChargedOff(entityId, amount, LocalDateTime.now()))
              .thenRun(_ => timerScheduler.startSingleTimer(timerKey,
                TakeOff(state.pricingPlan), Duration(chargePeriod, SECONDS)))

            case SetPricingPlan(name, price, replyTo) => Effect.persist(PricingPlanSet(entityId, name, price))
              .thenRun((_: State) => ctx.self ! TakeOff(calculateResidue(state.lastTakeOffDate, state.pricingPlan)))
              .thenRun(replyTo ! _)

            case Activate(replyTo) => Effect.persist(Activated(entityId))
              .thenRun(replyTo ! _)

            case Deactivate(replyTo) => Effect.persist(Deactivated(entityId))
              .thenRun(replyTo ! _)
          }
        }

        val eventHandler: (State, Event) => State = (state, event) => event match {
          case PaymentReceived(email, amount) => state.copy(
            accountBalance = state.accountBalance + amount,
            isActive = state.accountBalance + amount >= state.pricingPlan
          )

          case ChargedOff(email, amount, dateTime) => if (state.isActive) state.copy(
            accountBalance = state.accountBalance - amount,
            isActive = state.accountBalance - amount >= state.pricingPlan,
            lastTakeOffDate = dateTime
          ) else state

          case PricingPlanSet(email, name, price) => state.copy(name, price)

          case Activated(email) => state.copy(isActive = true)

          case Deactivated(email) => state.copy(isActive = false)
        }

        EventSourcedBehavior(
          PersistenceId(typeKey.name, entityId),
          State(pricingPlanName = "", pricingPlan = 0, accountBalance = 0, lastTakeOffDate = LocalDateTime.now(),
            isActive = true),
          commandHandler,
          eventHandler
        ).withRetention(RetentionCriteria.snapshotEvery(10, 3)
          .withDeleteEventsOnSnapshot)
          .withTagger(_ => Set(tag))
          .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1))
      }
    }
}
