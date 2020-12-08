package com.github.nikodemin.mobileoperator.actor

import java.io.File

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.MemberStatus
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import akka.cluster.typed.{Cluster, Join}
import akka.persistence.cassandra.testkit.CassandraLauncher
import akka.persistence.testkit.scaladsl.PersistenceInit.initializeDefaultPlugins
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
  private val testKit2 = ActorTestKit("UserActorSpec", ConfigFactory.load)

  private val systems = List(testKit.system, testKit2.system)

  override protected def beforeAll(): Unit = {
    CassandraLauncher.start(
      databaseDirectory,
      CassandraLauncher.DefaultTestConfigResource,
      clean = true,
      port = 19042,
      CassandraLauncher.classpathForResources("logback-test.xml"))

    // avoid concurrent creation of keyspace and tables
    initializePersistence()
    systems.foreach(system => {
      val sharding = ClusterSharding(system)
      sharding.init(Entity(UserActor.typeKey) { entityContext =>
        UserActor(sharding, entityContext.entityId)
      })
      sharding.init(Entity(AccountActor.typeKey) { entityContext =>
        AccountActor(entityContext.entityId)
      })
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

    testKit2.shutdownTestKit()
    testKit.shutdownTestKit()

    CassandraLauncher.stop()
    FileUtils.deleteDirectory(databaseDirectory)
  }

  private def generateEmail = Random.alphanumeric.filter(!_.isDigit).take(10).mkString

  "Account actor" should {
    "init and join Cluster" in {
      testKit.spawn[Nothing](Behaviors.empty, "guardian")
      testKit2.spawn[Nothing](Behaviors.empty, "guardian")

      systems.foreach { sys =>
        Cluster(sys).manager ! Join(Cluster(testKit.system).selfMember.address)
      }

      // let the nodes join and become Up
      eventually(PatienceConfiguration.Timeout(10.seconds)) {
        systems.foreach { sys =>
          Cluster(sys).selfMember.status should ===(MemberStatus.Up)
        }
      }
    }

    "create new user and account" in {
      val phoneNumber = "8 942 323 43 74"
      val pricingPlanName = "some plan"
      val pricingPlan = 500
      val email = generateEmail

      val accountProbe = testKit.createTestProbe[AccountActor.State]

      val account = ClusterSharding(testKit.system).entityRefFor(AccountActor.typeKey,
        AccountActor.entityId(phoneNumber))
      val user = ClusterSharding(testKit2.system).entityRefFor(UserActor.typeKey,
        UserActor.entityId(email))

      user ! UserActor.AddAccount(phoneNumber, pricingPlanName, pricingPlan)

      Thread.sleep(1000)

      account ! AccountActor.Get(accountProbe.ref)

      val actualState = accountProbe.receiveMessage()

      actualState.pricingPlanName should ===(pricingPlanName)
      actualState.pricingPlan should ===(pricingPlan)
    }

    "change user data" in {
      val email = generateEmail
      val firstName = "first name"
      val lastName = "last name"
      val dateOfBirth = null

      val probe = testKit2.createTestProbe[UserActor.State]

      val user = ClusterSharding(testKit.system).entityRefFor(UserActor.typeKey,
        UserActor.entityId(email))
      val user2 = ClusterSharding(testKit2.system).entityRefFor(UserActor.typeKey,
        UserActor.entityId(email))

      user ! UserActor.ChangeUserData(Some(firstName), Some(lastName), None)

      Thread.sleep(600)

      user2 ! UserActor.Get(probe.ref)

      val actualState = probe.receiveMessage()
      actualState.firstName should ===(firstName)
      actualState.lastName should ===(lastName)
      actualState.dateOfBirth should ===(dateOfBirth)
    }
  }
}
