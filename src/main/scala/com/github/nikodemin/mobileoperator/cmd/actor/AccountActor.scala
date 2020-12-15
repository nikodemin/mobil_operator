package com.github.nikodemin.mobileoperator.cmd.actor

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.delivery.ShardingProducerController.EntityId
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import akka.util.Timeout
import com.github.nikodemin.mobileoperator.common.serialization.CborSerializable
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

object AccountActor {

  sealed trait Command extends CborSerializable

  case class Payment(amount: Int, replyTo: Option[ActorRef[State]] = None) extends Command

  private case class TakeOff(amount: Int) extends Command

  case class SetPricingPlan(name: String, price: Int, replyTo: Option[ActorRef[State]] = None) extends Command

  case class Deactivate(replyTo: Option[ActorRef[State]] = None) extends Command

  case class Activate(replyTo: Option[ActorRef[State]] = None) extends Command


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

  def apply(entityId: EntityId)
           (implicit askTimeout: Timeout): Behavior[Command] =
    Behaviors.setup { ctx =>
      Behaviors.withTimers { timerScheduler =>
        val chargePeriod = ConfigFactory.load().getLong("mobile-operator.charge-period")
        val timerKey = "User actor"

        def calculateResidue(startDateTime: LocalDateTime, oldPrice: Int) =
          (ChronoUnit.SECONDS.between(startDateTime, LocalDateTime.now()) * oldPrice.toFloat / chargePeriod).toInt

        val commandHandler: (State, Command) => Effect[Event, State] = (state, cmd) => {

          cmd match {
            case Payment(amount, replyTo) => Effect.persist(PaymentReceived(entityId, amount))
              .thenRun((state: State) => replyTo.foreach(_ ! state))
              .thenRun(newState => if (newState.isActive && !state.isActive) {
                timerScheduler.startSingleTimer(timerKey, TakeOff(state.pricingPlan), Duration(chargePeriod, SECONDS))
                ctx.self ! Activate()
              })

            case TakeOff(amount) => if (state.isActive) {
              Effect.persist(ChargedOff(entityId, amount, LocalDateTime.now()))
                .thenRun { newState =>
                  if (newState.isActive) {
                    timerScheduler.startSingleTimer(timerKey,
                      TakeOff(state.pricingPlan), Duration(chargePeriod, SECONDS))
                  } else {
                    ctx.self ! Deactivate()
                  }
                }
            } else {
              Effect.none
            }

            case SetPricingPlan(name, price, replyTo) => Effect.persist(PricingPlanSet(entityId, name, price))
              .thenRun((_: State) => ctx.self ! TakeOff(calculateResidue(state.lastTakeOffDate, state.pricingPlan)))
              .thenRun(state => replyTo.foreach(_ ! state))

            case Activate(replyTo) => Effect.persist(Activated(entityId))
              .thenRun(state => replyTo.foreach(_ ! state))

            case Deactivate(replyTo) => Effect.persist(Deactivated(entityId))
              .thenRun(state => replyTo.foreach(_ ! state))
          }
        }

        val eventHandler: (State, Event) => State = (state, event) => event match {
          case PaymentReceived(email, amount) => state.copy(
            accountBalance = state.accountBalance + amount,
            isActive = state.accountBalance + amount >= state.pricingPlan
          )

          case ChargedOff(email, amount, dateTime) => state.copy(
            accountBalance = state.accountBalance - amount,
            isActive = state.accountBalance - amount >= state.pricingPlan,
            lastTakeOffDate = dateTime
          )

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
