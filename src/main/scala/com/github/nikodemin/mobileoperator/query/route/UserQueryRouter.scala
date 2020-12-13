package com.github.nikodemin.mobileoperator.query.route

import akka.http.scaladsl.server.Directives._enhanceRouteWithConcatenation
import akka.http.scaladsl.server.Route
import com.github.nikodemin.mobileoperator.common.route.interfaces.BaseRouter
import com.github.nikodemin.mobileoperator.common.route.util.Implicits._
import com.github.nikodemin.mobileoperator.query.model.dto.{UserQueryDto, UserResponseDto}
import com.github.nikodemin.mobileoperator.query.route.util.Implicits._
import com.github.nikodemin.mobileoperator.query.service.UserQueryService
import sttp.tapir.json.circe._
import sttp.tapir.server.akkahttp._
import sttp.tapir.{Endpoint, _}

import scala.concurrent.ExecutionContext

class UserQueryRouter(userService: UserQueryService)
                     (implicit executionContext: ExecutionContext) extends BaseRouter {
  override def route: Route = getByQueryRoute ~ getByEmailRoute

  override def endpoints: List[Endpoint[_, _, _, _]] = List(getByQuery, getByEmail)

  private val userEndpoint = endpoint.tag("User").in("user")

  private val getByQuery = userEndpoint
    .post
    .name("Get using query")
    .in("query")
    .in(jsonBody[UserQueryDto])
    .out(jsonBody[Seq[UserResponseDto]])

  private val getByQueryRoute = getByQuery.toRoute(userService.getByUserQuery)

  private val getByEmail = userEndpoint
    .get
    .name("Get by email")
    .in("email")
    .in(path[String]("email"))
    .out(jsonBody[Option[UserResponseDto]])

  private val getByEmailRoute = getByEmail.toRoute(userService.getByEmail)
}
