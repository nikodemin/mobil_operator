package com.github.nikodemin.mobileoperator.actor

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.delivery.ShardingProducerController.EntityId
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import com.github.nikodemin.mobileoperator.serialization.CborSerializable
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

object AccountActor {

  sealed trait Command extends CborSerializable

  case class Payment(amount: Int) extends Command

  case class TakeOff(amount: Int) extends Command

  case class SetPricingPlan(name: String, price: Int) extends Command

  object Deactivate extends Command

  object Activate extends Command

  case class Get(replyTo: ActorRef[State]) extends Command


  private sealed trait Event extends CborSerializable

  private case class PaymentReceived(amount: Int) extends Event

  private case class ChargedOff(amount: Int) extends Event

  private case class PricingPlanSet(name: String, price: Int) extends Event

  private object Deactivated extends Event

  private object Activated extends Event


  case class State(pricingPlanName: String, pricingPlan: Int, accountBalance: Long, lastTakeOffDate: LocalDateTime,
                   isActive: Boolean) extends CborSerializable

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("AccountActor")

  val tag = "Account actor"

  def entityId(phoneNumber: String) = s"$tag, phone number: $phoneNumber"

  def apply(entityId: EntityId): Behavior[Command] =
    Behaviors.setup { ctx =>
      Behaviors.withTimers { timerScheduler =>
        val chargePeriod = ConfigFactory.load().getLong("mobile-operator.charge-period")
        val timerKey = "User actor"

        def calculateResidue(startDateTime: LocalDateTime, oldPrice: Int) =
          (ChronoUnit.SECONDS.between(startDateTime, LocalDateTime.now()) * oldPrice / chargePeriod).toInt

        val commandHandler: (State, Command) => Effect[Event, State] = (state, cmd) => {

          cmd match {
            case Payment(amount) => Effect.persist(PaymentReceived(amount))

            case TakeOff(amount) => Effect.persist(ChargedOff(amount))
              .thenRun(_ => timerScheduler.startSingleTimer(timerKey,
                TakeOff(state.pricingPlan), Duration(chargePeriod, SECONDS)))

            case SetPricingPlan(name, price) => Effect.persist(PricingPlanSet(name, price))
              .thenRun(_ => ctx.self ! TakeOff(calculateResidue(state.lastTakeOffDate, state.pricingPlan)))

            case Activate => Effect.persist(Activated)

            case Deactivate => Effect.persist(Deactivated)

            case Get(replyTo) => Effect.none.thenRun(replyTo ! _)
          }
        }

        val eventHandler: (State, Event) => State = (state, event) => event match {
          case PaymentReceived(amount) => state.copy(
            accountBalance = state.accountBalance + amount,
            isActive = state.accountBalance + amount >= state.pricingPlan
          )

          case ChargedOff(amount) => if (state.isActive) state.copy(
            accountBalance = state.accountBalance - amount,
            isActive = state.accountBalance - amount >= state.pricingPlan,
            lastTakeOffDate = LocalDateTime.now()
          ) else state

          case PricingPlanSet(name, price) => state.copy(name, price)

          case Activated => state.copy(isActive = true)

          case Deactivated => state.copy(isActive = false)
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
