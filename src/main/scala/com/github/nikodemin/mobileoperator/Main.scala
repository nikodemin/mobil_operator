package com.github.nikodemin.mobileoperator

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorSystem => ClassicActorSystem}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, ShardedDaemonProcess}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardedDaemonProcessSettings}
import akka.cluster.typed.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.management.scaladsl.AkkaManagement
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.projection.cassandra.scaladsl.CassandraProjection
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.{ProjectionBehavior, ProjectionId}
import akka.stream.alpakka.cassandra.scaladsl.CassandraSessionRegistry
import akka.util.Timeout
import com.github.nikodemin.mobileoperator.cmd.actor.{AccountActor, UserActor}
import com.github.nikodemin.mobileoperator.cmd.route.{AccountRouter, UserRouter}
import com.github.nikodemin.mobileoperator.cmd.service._
import com.github.nikodemin.mobileoperator.query.config.DbConfig
import com.github.nikodemin.mobileoperator.query.dao.{AccountDao, UserDao}
import com.github.nikodemin.mobileoperator.query.handler.{AccountHandler, UserHandler}
import com.github.nikodemin.mobileoperator.query.projection.{AccountProjectionHandler, UserProjectionHandler}
import com.github.nikodemin.mobileoperator.query.route.{AccountQueryRouter, UserQueryRouter}
import com.github.nikodemin.mobileoperator.query.service.{AccountQueryService, UserQueryService}
import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway
import slick.jdbc.PostgresProfile.api._
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.swagger.akkahttp.SwaggerAkka

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.io.StdIn

object Main {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem(Behaviors.empty, "mobile-operator-cluster")
    val sharding = ClusterSharding(system)
    val cluster = Cluster(system)
    implicit val classicSystem: ClassicActorSystem = system.toClassic
    import classicSystem.dispatcher

    val serverPort = ConfigFactory.load().getInt("mobile-operator.server-port")

    implicit val askTimeout: Timeout = Timeout(10.seconds)

    if (cluster.selfMember.roles("management")) {
      AkkaManagement(system).start()
    }

    sharding.init(Entity(AccountActor.typeKey) { entityContext =>
      AccountActor(entityContext.entityId)
    })

    sharding.init(Entity(UserActor.typeKey) { entityContext =>
      UserActor(sharding, entityContext.entityId)
    })

    val (routes, db) = if (cluster.selfMember.roles("cmd")) {
      initCommand(sharding)
    } else if (cluster.selfMember.roles("query")) {
      initQuery(system)
    } else {
      throw new IllegalStateException("Unsupported role")
    }

    val binding = Http().newServerAt("localhost", serverPort)
      .bind(routes)

    binding.foreach(b => println(s"Binding on ${b.localAddress}"))

    StdIn.readLine()

    binding.flatMap(_.unbind()).onComplete(_ => {
      db.foreach(_.close())
      classicSystem.terminate
      system.terminate
    })
  }

  def initCommand(sharding: ClusterSharding)
                 (implicit classicSystem: ClassicActorSystem, executionContext: ExecutionContext, askTimeout: Timeout): (Route, Option[Database]) = {
    val accountService = new AccountService(sharding)
    val userService = new UserService(sharding)

    val accountRouter = new AccountRouter(accountService)
    val userRouter = new UserRouter(userService)


    val openApiYaml = (accountRouter.endpoints ++ userRouter.endpoints)
      .toOpenAPI("Mobile operator command", "1.0.0").toYaml

    (accountRouter.route ~ (new SwaggerAkka(openApiYaml)).routes ~ userRouter.route, None)
  }

  def initQuery(system: ActorSystem[Nothing])
               (implicit executionContext: ExecutionContext): (Route, Option[Database]) = {
    createTables(system)

    val config = DbConfig.default

    implicit val db: Database = Database.forURL(config.dbUrl, config.dbUserName, config.dbPassword)

    Flyway
      .configure()
      .dataSource(config.dbUrl, config.dbUserName, config.dbPassword)
      .load()
      .migrate()

    val userSourceProvider = EventSourcedProvider
      .eventsByTag[UserActor.Event](
        system,
        readJournalPluginId = CassandraReadJournal.Identifier,
        tag = UserActor.tag)
    val accountSourceProvider = EventSourcedProvider
      .eventsByTag[AccountActor.Event](
        system,
        readJournalPluginId = CassandraReadJournal.Identifier,
        tag = AccountActor.tag)

    val userDao = new UserDao
    val accountDao = new AccountDao

    val userProjection = CassandraProjection.atLeastOnce(
      projectionId = ProjectionId("users", UserActor.tag),
      userSourceProvider,
      handler = () => new UserProjectionHandler(new UserHandler(userDao, accountDao)))
    val accountProjection = CassandraProjection.atLeastOnce(
      projectionId = ProjectionId("accounts", AccountActor.tag),
      accountSourceProvider,
      handler = () => new AccountProjectionHandler(new AccountHandler(accountDao))
    )

    val shardingSettings = ClusterShardingSettings(system)
    val shardedDaemonProcessSettings =
      ShardedDaemonProcessSettings(system).withShardingSettings(shardingSettings.withRole("query"))

    ShardedDaemonProcess(system).init(
      name = "users",
      1,
      _ => ProjectionBehavior(userProjection),
      shardedDaemonProcessSettings,
      Some(ProjectionBehavior.Stop))

    ShardedDaemonProcess(system).init(
      name = "accounts",
      1,
      _ => ProjectionBehavior(accountProjection),
      shardedDaemonProcessSettings,
      Some(ProjectionBehavior.Stop))

    val userService = new UserQueryService(userDao)
    val accountService = new AccountQueryService(accountDao)

    val userRouter = new UserQueryRouter(userService)
    val accountRouter = new AccountQueryRouter(accountService)

    val openApiYaml = (accountRouter.endpoints ++ userRouter.endpoints)
      .toOpenAPI("Mobile operator query", "1.0.0").toYaml

    (accountRouter.route ~ (new SwaggerAkka(openApiYaml)).routes ~ userRouter.route, Some(db))
  }

  def createTables(system: ActorSystem[_]): Unit = {

    val session =
      CassandraSessionRegistry(system).sessionFor("alpakka.cassandra")

    val keyspaceStmt =
      """
      CREATE KEYSPACE IF NOT EXISTS mobile_operator_offset
      WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 }
      """

    val offsetTableStmt =
      """
      CREATE TABLE IF NOT EXISTS mobile_operator_offset.offset_store (
        projection_name text,
        partition int,
        projection_key text,
        offset text,
        manifest text,
        last_updated timestamp,
        PRIMARY KEY ((projection_name, partition), projection_key)
      )
      """
    Await.ready(session.executeDDL(keyspaceStmt), 10.seconds)
    Await.ready(session.executeDDL(offsetTableStmt), 10.seconds)
  }
}
