package com.github.nikodemin.mobileoperator

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorSystem => ClassicActorSystem}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import akka.cluster.typed.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.management.scaladsl.AkkaManagement
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.util.Timeout
import com.github.nikodemin.mobileoperator.actor.{AccountActor, UserActor}
import com.github.nikodemin.mobileoperator.route.{AccountRouter, UserRouter}
import com.github.nikodemin.mobileoperator.service._
import com.typesafe.config.ConfigFactory
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.swagger.akkahttp.SwaggerAkka

import scala.concurrent.duration._
import scala.io.StdIn

object Main {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem(Behaviors.empty, "mobile-operator-cluster")
    val sharding = ClusterSharding(system)
    val cluster = Cluster(system)

    val serverPort = ConfigFactory.load().getInt("mobile-operator.server-port")

    if (cluster.selfMember.roles("management")) {
      AkkaManagement(system).start()
    }

    sharding.init(Entity(AccountActor.typeKey) { entityContext =>
      AccountActor(entityContext.entityId)
    })

    sharding.init(Entity(UserActor.typeKey) { entityContext =>
      UserActor(sharding, entityContext.entityId)
    })

    implicit val classicSystem: ClassicActorSystem = system.toClassic
    import classicSystem.dispatcher

    implicit val askTimeout: Timeout = Timeout(1.second)

    val journal = PersistenceQuery.get(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

    val accountService = new AccountService(sharding)
    val userService = new UserService(sharding, journal)

    val accountRouter = new AccountRouter(accountService)
    val userRouter = new UserRouter(userService)


    val openApiYaml = (accountRouter.endpoints ++ userRouter.endpoints)
      .toOpenAPI("Mobile operator", "1.0.0").toYaml

    val binding = Http().newServerAt("localhost", serverPort)
      .bind(accountRouter.route ~ (new SwaggerAkka(openApiYaml)).routes ~ userRouter.route)

    binding.foreach(b => println(s"Binding on ${b.localAddress}"))

    StdIn.readLine()

    binding.flatMap(_.unbind()).onComplete(_ => {
      classicSystem.terminate
      system.terminate
    })
  }
}
