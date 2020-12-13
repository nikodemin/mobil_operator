package com.github.nikodemin.mobileoperator.cmd.route

import akka.http.scaladsl.server.Directives._enhanceRouteWithConcatenation
import akka.http.scaladsl.server.Route
import com.github.nikodemin.mobileoperator.cmd.model.dto.{AccountAddDto, UserAddDto, UserChangeDto, UserResponseDto}
import com.github.nikodemin.mobileoperator.cmd.route.util.Implicits._
import com.github.nikodemin.mobileoperator.cmd.service.UserService
import com.github.nikodemin.mobileoperator.common.route.interfaces.BaseRouter
import com.github.nikodemin.mobileoperator.common.route.util.Implicits._
import sttp.tapir.json.circe._
import sttp.tapir.server.akkahttp._
import sttp.tapir.{Endpoint, _}

import scala.concurrent.ExecutionContext

class UserRouter(userService: UserService)(implicit executionContext: ExecutionContext) extends BaseRouter {
  override lazy val route: Route = addUserRoute ~ changeUserRoute ~ addAccountRoute
  override lazy val endpoints: List[Endpoint[_, _, _, _]] = List(addUser, changeUser, addAccount)

  private val userEndpoint = endpoint.in("user").tag("user")

  private val addAccount = userEndpoint
    .post
    .name("Add account")
    .in("add account")
    .in(jsonBody[AccountAddDto])
    .out(jsonBody[UserResponseDto])

  private val addAccountRoute = addAccount.toRoute(userService.addAccount)

  private val addUser = userEndpoint
    .post
    .name("Add user")
    .in(jsonBody[UserAddDto])
    .out(jsonBody[UserResponseDto])

  private val addUserRoute = addUser.toRoute(userService.addUser)

  private val changeUser = userEndpoint
    .put
    .name("Change user")
    .in("email")
    .in(path[String]("email"))
    .in(jsonBody[UserChangeDto])
    .out(jsonBody[UserResponseDto])

  private val changeUserRoute = changeUser.toRoute(entry => userService.changeUser(entry._1, entry._2))
}
