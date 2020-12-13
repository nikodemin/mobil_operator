package com.github.nikodemin.mobileoperator.query.route

import akka.http.scaladsl.server.Directives._enhanceRouteWithConcatenation
import akka.http.scaladsl.server.Route
import com.github.nikodemin.mobileoperator.common.route.interfaces.BaseRouter
import com.github.nikodemin.mobileoperator.common.route.util.Implicits._
import com.github.nikodemin.mobileoperator.query.model.dto.{AccountGetByLastTakeOffDate, AccountQueryDto, AccountResponseDto}
import com.github.nikodemin.mobileoperator.query.route.util.Implicits._
import com.github.nikodemin.mobileoperator.query.service.AccountQueryService
import sttp.tapir.json.circe._
import sttp.tapir.server.akkahttp._
import sttp.tapir.{Endpoint, _}

import scala.concurrent.ExecutionContext

class AccountQueryRouter(accountService: AccountQueryService)
                        (implicit executionContext: ExecutionContext) extends BaseRouter {
  override def route: Route = getByQueryRoute ~ getByLastTakeOffDateBetweenRoute

  override def endpoints: List[Endpoint[_, _, _, _]] = List(getByQuery, getByLastTakeOffDateBetween)

  private val accountEndpoint = endpoint.tag("Account").in("account")

  private val getByQuery = accountEndpoint
    .post
    .name("Get using query")
    .in("query")
    .in(jsonBody[AccountQueryDto])
    .out(jsonBody[Seq[AccountResponseDto]])

  private val getByQueryRoute = getByQuery.toRoute(accountService.getByQueryDto)

  private val getByLastTakeOffDateBetween = accountEndpoint
    .post
    .name("Get by last take off date")
    .in("take off date")
    .in(jsonBody[AccountGetByLastTakeOffDate])
    .out(jsonBody[Seq[AccountResponseDto]])

  private val getByLastTakeOffDateBetweenRoute = getByLastTakeOffDateBetween.toRoute(accountService.getByLastTakeOffDateBetween)
}
