package com.github.nikodemin.mobileoperator.query.route

import akka.http.scaladsl.server.Route
import com.github.nikodemin.mobileoperator.common.route.interfaces.BaseRouter
import com.github.nikodemin.mobileoperator.query.service.UserQueryService
import sttp.tapir.server.akkahttp._
import sttp.tapir.{Endpoint, _}

import scala.concurrent.{ExecutionContext, Future}

class UserQueryRouter(userService: UserQueryService)
                     (implicit executionContext: ExecutionContext) extends BaseRouter {
  override def route: Route = userEndpoint.toRoute(_ => Future(Left(())))

  override def endpoints: List[Endpoint[_, _, _, _]] = List(userEndpoint)

  private val userEndpoint = endpoint.tag("User").in("user")
}
