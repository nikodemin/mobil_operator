package com.github.nikodemin.mobileoperator.route

import akka.http.scaladsl.server.Directives._enhanceRouteWithConcatenation
import akka.http.scaladsl.server.Route
import com.github.nikodemin.mobileoperator.model.dto.{UserAddDto, UserChangeDto, UserGetDto}
import com.github.nikodemin.mobileoperator.route.interfaces.BaseRouter
import com.github.nikodemin.mobileoperator.route.util.Implicits._
import com.github.nikodemin.mobileoperator.service.UserService
import sttp.tapir.json.circe._
import sttp.tapir.server.akkahttp._
import sttp.tapir.{Endpoint, _}

import scala.concurrent.ExecutionContext

class UserRouter(userService: UserService)(implicit executionContext: ExecutionContext) extends BaseRouter {
  override lazy val route: Route = addUserRoute ~ getUserByEmailRoute ~ changeUserRoute
  override lazy val endpoints: List[Endpoint[_, _, _, _]] = List(addUser, getUserByEmail, changeUser)

  private val userEndpoint = endpoint.in("user").tag("user")

  private val addUser = userEndpoint
    .post
    .in(jsonBody[UserAddDto])
    .out(jsonBody[Boolean])

  private val addUserRoute = addUser.toRoute(userService.addUser)

  private val getUserByEmail = userEndpoint
    .get
    .in("email")
    .in(path[String]("email"))
    .out(jsonBody[UserGetDto])

  private val getUserByEmailRoute = getUserByEmail.toRoute(userService.getUserByEmail)

  private val changeUser = userEndpoint
    .put
    .in("email")
    .in(path[String]("email"))
    .in(jsonBody[UserChangeDto])
    .out(jsonBody[Boolean])

  private val changeUserRoute = changeUser.toRoute(entry => userService.changeUser(entry._1, entry._2))
}
