package com.github.nikodemin.mobileoperator.query.route

import akka.http.scaladsl.server.Route
import com.github.nikodemin.mobileoperator.common.route.interfaces.BaseRouter
import com.github.nikodemin.mobileoperator.query.service.AccountQueryService
import sttp.tapir.server.akkahttp._
import sttp.tapir.{Endpoint, _}

import scala.concurrent.{ExecutionContext, Future}

class AccountQueryRouter(accountService: AccountQueryService)
                        (implicit executionContext: ExecutionContext) extends BaseRouter {
  override def route: Route = accountEndpoint.toRoute(_ => Future(Left(())))

  override def endpoints: List[Endpoint[_, _, _, _]] = List(accountEndpoint)

  private val accountEndpoint = endpoint.tag("Account").in("account")
}
