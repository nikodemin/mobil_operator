package com.github.nikodemin.mobileoperator.cmd.route

import akka.http.scaladsl.server.Directives._enhanceRouteWithConcatenation
import akka.http.scaladsl.server.Route
import com.github.nikodemin.mobileoperator.cmd.model.dto.{AccountAddDto, AccountGetDto, SetPricingPlanDto}
import com.github.nikodemin.mobileoperator.cmd.route.util.Implicits._
import com.github.nikodemin.mobileoperator.cmd.service.AccountService
import com.github.nikodemin.mobileoperator.common.route.interfaces.BaseRouter
import com.github.nikodemin.mobileoperator.common.route.util.Implicits._
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.akkahttp._

import scala.concurrent.ExecutionContext

class AccountRouter(accountService: AccountService)(implicit executionContext: ExecutionContext) extends BaseRouter {
  override lazy val endpoints: List[Endpoint[_, _, _, _]] = List(addAccount, activateAccount, deactivateAccount,
    getByPhoneNumber, pay, setPricingPlan)
  override lazy val route: Route = addAccountRoute ~ activateAccountRoute ~ deactivateAccountRoute ~
    getByPhoneNumberRoute ~ payRoute ~ setPricingPlanRoute

  private val accountEndpoint = endpoint.in("account").tag("account")

  private val addAccount = accountEndpoint
    .post
    .in(jsonBody[AccountAddDto])
    .out(jsonBody[Boolean])

  private val addAccountRoute = addAccount.toRoute(accountService.addAccount)

  private val activateAccount = accountEndpoint
    .put
    .in("activate")
    .in("phoneNumber")
    .in(path[String]("phoneNumber"))
    .out(jsonBody[Boolean])

  private val activateAccountRoute = activateAccount.toRoute(accountService.activateAccount)

  private val deactivateAccount = accountEndpoint
    .put
    .in("deactivate")
    .in("phoneNumber")
    .in(path[String]("phoneNumber"))
    .out(jsonBody[Boolean])

  private val deactivateAccountRoute = deactivateAccount.toRoute(accountService.deactivateAccount)

  private val getByPhoneNumber = accountEndpoint
    .get
    .in("phoneNumber")
    .in(path[String]("phoneNumber"))
    .out(jsonBody[AccountGetDto])

  private val getByPhoneNumberRoute = getByPhoneNumber.toRoute(accountService.getByPhoneNumber)

  private val pay = accountEndpoint
    .put
    .in("pay")
    .in("phoneNumber")
    .in(path[String]("phoneNumber"))
    .in("amount")
    .in(path[Int]("amount"))
    .out(jsonBody[Boolean])

  private val payRoute = pay.toRoute(entry => accountService.pay(entry._1, entry._2))

  private val setPricingPlan = accountEndpoint
    .put
    .in("pricingPlan")
    .in("phoneNumber")
    .in(path[String]("phoneNumber"))
    .in(jsonBody[SetPricingPlanDto])
    .out(jsonBody[Boolean])

  private val setPricingPlanRoute = setPricingPlan.toRoute(entry => accountService.setPricingPlan(entry._1, entry._2))

}
