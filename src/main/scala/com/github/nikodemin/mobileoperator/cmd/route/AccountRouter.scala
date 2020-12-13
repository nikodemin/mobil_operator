package com.github.nikodemin.mobileoperator.cmd.route

import akka.http.scaladsl.server.Directives._enhanceRouteWithConcatenation
import akka.http.scaladsl.server.Route
import com.github.nikodemin.mobileoperator.cmd.model.dto.{AccountResponseDto, SetPricingPlanDto}
import com.github.nikodemin.mobileoperator.cmd.route.util.Implicits._
import com.github.nikodemin.mobileoperator.cmd.service.AccountService
import com.github.nikodemin.mobileoperator.common.route.interfaces.BaseRouter
import com.github.nikodemin.mobileoperator.common.route.util.Implicits._
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.akkahttp._

import scala.concurrent.ExecutionContext

class AccountRouter(accountService: AccountService)(implicit executionContext: ExecutionContext) extends BaseRouter {
  override lazy val endpoints: List[Endpoint[_, _, _, _]] = List(activateAccount, deactivateAccount,
    pay, setPricingPlan)
  override lazy val route: Route = activateAccountRoute ~ deactivateAccountRoute ~
    payRoute ~ setPricingPlanRoute

  private val accountEndpoint = endpoint.in("account").tag("account")

  private val activateAccount = accountEndpoint
    .put
    .name("Activate account")
    .in("activate")
    .in("phoneNumber")
    .in(path[String]("phoneNumber"))
    .out(jsonBody[AccountResponseDto])

  private val activateAccountRoute = activateAccount.toRoute(accountService.activateAccount)

  private val deactivateAccount = accountEndpoint
    .put
    .name("Deactivate account")
    .in("deactivate")
    .in("phoneNumber")
    .in(path[String]("phoneNumber"))
    .out(jsonBody[AccountResponseDto])

  private val deactivateAccountRoute = deactivateAccount.toRoute(accountService.deactivateAccount)

  private val pay = accountEndpoint
    .put
    .name("Pay")
    .in("pay")
    .in("phoneNumber")
    .in(path[String]("phoneNumber"))
    .in("amount")
    .in(path[Int]("amount"))
    .out(jsonBody[AccountResponseDto])

  private val payRoute = pay.toRoute(entry => accountService.pay(entry._1, entry._2))

  private val setPricingPlan = accountEndpoint
    .put
    .name("Set pricing plan")
    .in("pricingPlan")
    .in("phoneNumber")
    .in(path[String]("phoneNumber"))
    .in(jsonBody[SetPricingPlanDto])
    .out(jsonBody[AccountResponseDto])

  private val setPricingPlanRoute = setPricingPlan.toRoute(entry => accountService.setPricingPlan(entry._1, entry._2))

}
