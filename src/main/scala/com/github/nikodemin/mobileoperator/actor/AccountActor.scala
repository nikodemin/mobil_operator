package com.github.nikodemin.mobileoperator.actor

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

object AccountActor {

  sealed trait Command

  case class Payment(amount: Int) extends Command

  case class TakeOff(amount: Int) extends Command

  case class SetPricingPlan(name: String, price: Int) extends Command

  object Deactivate extends Command

  object Activate extends Command

  case class Get(replyTo: ActorRef[State]) extends Command


  private sealed trait Event

  private case class PaymentReceived(amount: Int) extends Event

  private case class ChargedOff(amount: Int) extends Event

  private case class PricingPlanSet(name: String, price: Int) extends Event

  private object Deactivated extends Event

  private object Activated extends Event


  case class State(pricingPlanName: String, pricingPlan: Int, accountBalance: Long, lastTakeOffDate: LocalDateTime,
                   isActive: Boolean)

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("AccountActor")

  def entityId(phoneNumber: String) = s"Account actor: $phoneNumber"

  def entityIdQuery(phoneNumber: String) = s"Account actor query: $phoneNumber"

  def apply(phoneNumber: String, pricingPlanName: String, pricingPlan: Int,
            sharding: ClusterSharding, isQuery: Boolean): Behavior[Command] =
    Behaviors.setup { ctx =>

      val chargePeriod = ConfigFactory.load("mobile-operator").getLong("charge-period")

      def calculateResidue(startDateTime: LocalDateTime, oldPrice: Int) =
        (ChronoUnit.SECONDS.between(startDateTime, LocalDateTime.now()) * oldPrice / chargePeriod).toInt

      val commandHandler: (State, Command) => Effect[Event, State] = (state, cmd) => {
        if (!isQuery) {
          val query = sharding.entityRefFor(AccountActor.typeKey, AccountActor.entityIdQuery(phoneNumber))
          query ! cmd
        }

        cmd match {
          case Payment(amount) => Effect.persist(PaymentReceived(amount))

          case TakeOff(amount) => Effect.persist(ChargedOff(amount))

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
          isActive = state.accountBalance + amount > 0
        )

        case ChargedOff(amount) => if (state.isActive) state.copy(
          accountBalance = state.accountBalance - amount,
          isActive = state.accountBalance > amount,
          lastTakeOffDate = LocalDateTime.now()
        ) else state

        case PricingPlanSet(name, price) => state.copy(name, price)

        case Activated => state.copy(isActive = true)

        case Deactivated => state.copy(isActive = false)
      }

      EventSourcedBehavior(
        PersistenceId(typeKey.name, if (isQuery) entityIdQuery(phoneNumber) else entityId(phoneNumber)),
        State(pricingPlanName, pricingPlan, accountBalance = 0, lastTakeOffDate = null, isActive = true),
        commandHandler,
        eventHandler
      ).withRetention(RetentionCriteria.snapshotEvery(10, 3).withDeleteEventsOnSnapshot)
        .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1))
    }
}
