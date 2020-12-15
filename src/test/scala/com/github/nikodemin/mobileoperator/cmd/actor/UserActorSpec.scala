package com.github.nikodemin.mobileoperator.cmd.actor

import java.io.File
import java.time.LocalDate

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.MemberStatus
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import akka.cluster.typed.{Cluster, Join}
import akka.persistence.cassandra.testkit.CassandraLauncher
import akka.persistence.testkit.scaladsl.PersistenceInit.initializeDefaultPlugins
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FileUtils
import org.scalatest.concurrent.{Eventually, PatienceConfiguration, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, TestSuite}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

class UserActorSpec
  extends TestSuite
    with Matchers
    with BeforeAndAfterAll
    with AnyWordSpecLike
    with ScalaFutures
    with Eventually {

  implicit private val patience: PatienceConfig =
    PatienceConfig(3.seconds, Span(100, org.scalatest.time.Millis))

  private val databaseDirectory = new File("target/cassandra-UserActorSpec")

  // one TestKit (ActorSystem) per cluster node
  private val testKit = ActorTestKit("UserActorSpec", ConfigFactory.load)
  private val probe = testKit.createTestProbe[UserActor.State]

  override protected def beforeAll(): Unit = {
    implicit val askTimeout: Timeout = Timeout(5.seconds)
    import scala.concurrent.ExecutionContext.Implicits.global

    CassandraLauncher.start(
      databaseDirectory,
      CassandraLauncher.DefaultTestConfigResource,
      clean = true,
      port = 19042,
      CassandraLauncher.classpathForResources("logback-test.xml"))

    initializePersistence()
    val sharding = ClusterSharding(testKit.system)
    sharding.init(Entity(UserActor.typeKey) { entityContext =>
      UserActor(sharding, entityContext.entityId)
    })
    sharding.init(Entity(AccountActor.typeKey) { entityContext =>
      AccountActor(entityContext.entityId)
    })

    super.beforeAll()
  }

  private def initializePersistence(): Unit = {
    val timeout = 10.seconds
    val done = initializeDefaultPlugins(testKit.system, timeout)
    Await.result(done, timeout)
  }

  override protected def afterAll(): Unit = {
    super.afterAll()

    testKit.shutdownTestKit()

    CassandraLauncher.stop()
    FileUtils.deleteDirectory(databaseDirectory)
  }

  private def generateEmail = Random.alphanumeric.filter(!_.isDigit).take(10).mkString

  "User actor" should {
    "init and join Cluster" in {
      testKit.spawn[Nothing](Behaviors.empty, "guardian")

      Cluster(testKit.system).manager ! Join(Cluster(testKit.system).selfMember.address)

      // let the nodes join and become Up
      eventually(PatienceConfiguration.Timeout(10.seconds)) {
        Cluster(testKit.system).selfMember.status should ===(MemberStatus.Up)
      }
    }

    "not create new account if not initialized" in {
      val phoneNumber = "8 942 323 43 74"
      val pricingPlanName = "some plan"
      val pricingPlan = 500
      val email = generateEmail

      val user = ClusterSharding(testKit.system).entityRefFor(UserActor.typeKey,
        UserActor.entityId(email))

      user ! UserActor.AddAccount(phoneNumber, pricingPlanName, pricingPlan, probe.ref)

      val actualState = probe.receiveMessage()

      actualState.isInitialized should ===(false)
      actualState.phoneNumbers should not contain phoneNumber
    }

    "create new account if initialized" in {
      val phoneNumber = "8 942 323 43 74"
      val pricingPlanName = "some plan"
      val pricingPlan = 500
      val email = generateEmail

      val user = ClusterSharding(testKit.system).entityRefFor(UserActor.typeKey,
        UserActor.entityId(email))

      val firstName = "first name"
      val lastName = "last name"
      val dateOfBirth = LocalDate.now()

      user ! UserActor.ChangeUserData(Some(firstName), Some(lastName), Some(dateOfBirth), probe.ref)

      probe.expectMessageType[UserActor.State]

      user ! UserActor.AddAccount(phoneNumber, pricingPlanName, pricingPlan, probe.ref)

      val actualState = probe.receiveMessage()

      actualState.isInitialized should ===(true)
      actualState.phoneNumbers should contain(phoneNumber)
    }

    "change user data" in {
      val email = generateEmail
      val firstName = "first name"
      val lastName = "last name"
      val dateOfBirth = LocalDate.now()

      val user = ClusterSharding(testKit.system).entityRefFor(UserActor.typeKey,
        UserActor.entityId(email))

      user ! UserActor.ChangeUserData(Some(firstName), Some(lastName), Some(dateOfBirth), probe.ref)

      val actualState = probe.receiveMessage()
      actualState.firstName should ===(firstName)
      actualState.lastName should ===(lastName)
      actualState.dateOfBirth should ===(dateOfBirth)
    }
  }
}
