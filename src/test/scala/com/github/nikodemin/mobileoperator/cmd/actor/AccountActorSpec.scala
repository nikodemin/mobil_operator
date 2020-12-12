package com.github.nikodemin.mobileoperator.cmd.actor

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

class AccountActorSpec
  extends TestSuite
    with Matchers
    with BeforeAndAfterAll
    with AnyWordSpecLike
    with ScalaFutures
    with Eventually {

  implicit private val patience: PatienceConfig =
    PatienceConfig(3.seconds, Span(100, org.scalatest.time.Millis))

  private val databaseDirectory = new File("target/cassandra-AccountActorSpec")

  // one TestKit (ActorSystem) per cluster node
  private val testKit = ActorTestKit("AccountActorSpec", ConfigFactory.load)
  private val testKit2 = ActorTestKit("AccountActorSpec", ConfigFactory.load)

  private val systems = List(testKit.system, testKit2.system)

  private val probe = testKit.createTestProbe[AccountActor.State]()

  override protected def beforeAll(): Unit = {
    CassandraLauncher.start(
      databaseDirectory,
      CassandraLauncher.DefaultTestConfigResource,
      clean = true,
      port = 19042,
      CassandraLauncher.classpathForResources("logback-test.xml"))

    // avoid concurrent creation of keyspace and tables
    initializePersistence()
    systems.foreach(system => ClusterSharding(system).init(Entity(AccountActor.typeKey) { entityContext =>
      AccountActor(entityContext.entityId)
    }))
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

  private def generatePhoneNumber = Random.alphanumeric.filter(_.isDigit).take(11).mkString

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

    "create account and get it from different node" in {
      val pricingPlanName = "some pricicng plan"
      val pricingPlan = 500
      val phoneNumber = generatePhoneNumber

      val account = ClusterSharding(testKit.system).entityRefFor(AccountActor.typeKey,
        AccountActor.entityId(phoneNumber))
      val account2 = ClusterSharding(testKit2.system).entityRefFor(AccountActor.typeKey,
        AccountActor.entityId(phoneNumber))

      account2 ! AccountActor.SetPricingPlan(pricingPlanName, pricingPlan)

      Thread.sleep(500)

      account ! AccountActor.Get(probe.ref)

      val actual = probe.receiveMessage()

      actual.pricingPlanName should ===(pricingPlanName)
      actual.pricingPlan should ===(pricingPlan)
    }

    "take charge after configured period" in {
      val phoneNumber = generatePhoneNumber
      val pricingPlan = 300
      val payment = 2000
      val expectedBalance = payment - pricingPlan

      val account = ClusterSharding(testKit.system).entityRefFor(AccountActor.typeKey,
        AccountActor.entityId(phoneNumber))
      val account2 = ClusterSharding(testKit2.system).entityRefFor(AccountActor.typeKey,
        AccountActor.entityId(phoneNumber))

      account ! AccountActor.SetPricingPlan("plan", pricingPlan)
      account2 ! AccountActor.Payment(payment)

      Thread.sleep(4000)

      account ! AccountActor.Get(probe.ref)

      probe.receiveMessage(2.seconds).accountBalance should ===(expectedBalance)
    }

    "activate and deactivate account" in {
      val phoneNumber = generatePhoneNumber
      val account = ClusterSharding(testKit.system).entityRefFor(AccountActor.typeKey,
        AccountActor.entityId(phoneNumber))
      val account2 = ClusterSharding(testKit2.system).entityRefFor(AccountActor.typeKey,
        AccountActor.entityId(phoneNumber))

      account2 ! AccountActor.Deactivate
      Thread.sleep(300)

      account ! AccountActor.Get(probe.ref)
      probe.receiveMessage().isActive should ===(false)

      account2 ! AccountActor.Activate
      Thread.sleep(300)

      account ! AccountActor.Get(probe.ref)
      probe.receiveMessage().isActive should ===(true)
    }
  }
}
