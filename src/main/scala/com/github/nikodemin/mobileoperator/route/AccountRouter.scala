package com.github.nikodemin.mobileoperator.route

import akka.http.scaladsl.server.Route
import com.github.nikodemin.mobileoperator.model.dto.AddAccountDto
import com.github.nikodemin.mobileoperator.route.interfaces.BaseRouter
import com.github.nikodemin.mobileoperator.route.util.Implicits._
import com.github.nikodemin.mobileoperator.service.AccountService
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.akkahttp._

import scala.concurrent.ExecutionContext

class AccountRouter(accountService: AccountService)(implicit executionContext: ExecutionContext) extends BaseRouter {
  override lazy val endpoints: List[Endpoint[_, _, _, _]] = List(addAccount)
  override lazy val route: Route = addAccountRoute

  private val accountEndpoint = endpoint.in("account").tag("account")

  private val addAccount = accountEndpoint
    .post
    .in(jsonBody[AddAccountDto])
    .out(jsonBody[Boolean])

  private val addAccountRoute = addAccount.toRoute(accountService.addAccount)

}
