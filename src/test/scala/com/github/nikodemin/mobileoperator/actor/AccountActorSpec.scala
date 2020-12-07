package com.github.nikodemin.mobileoperator.actor

import java.io.File

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.MemberStatus
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import akka.cluster.typed.{Cluster, Join}
import akka.persistence.cassandra.testkit.CassandraLauncher
import akka.persistence.testkit.scaladsl.PersistenceInit.initializeDefaultPlugins
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.io.FileUtils
import org.scalatest.concurrent.{Eventually, PatienceConfiguration, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, TestSuite}

import scala.concurrent.Await
import scala.concurrent.duration._

object AccountActorSpec {
  val config: Config = ConfigFactory.parseString(
    s"""
      akka {
        remote {
          artery {
            transport = tcp
            canonical {
              hostname = 127.0.0.1
              port = 0
            }
          }
        }
      }

      akka.cluster {
         seed-nodes = []
      }

      akka.persistence.cassandra {
        events-by-tag.enabled = false
        query {
          refresh-interval = 500 ms
        }

        journal.keyspace-autocreate = on
        journal.tables-autocreate = on
        snapshot.keyspace-autocreate = on
        snapshot.tables-autocreate = on
      }
      datastax-java-driver {
        basic.contact-points = ["127.0.0.1:19042"]
        basic.load-balancing-policy.local-datacenter = "datacenter1"
      }

      event-processor {
        keep-alive-interval = 1 seconds
      }
      akka.loglevel = DEBUG
      akka.actor.testkit.typed.single-expect-default = 5s
      # For LoggingTestKit
      akka.actor.testkit.typed.filter-leeway = 5s
      akka.actor.testkit.typed.throw-on-shutdown-timeout = off
    """).withFallback(ConfigFactory.load())
}

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
  private val testKit = ActorTestKit("AccountActorSpec", AccountActorSpec.config)
  private val testKit2 = ActorTestKit("AccountActorSpec", AccountActorSpec.config)

  private val systems = List(testKit.system, testKit2.system)

  private val phoneNumber = "8 990 333 94 23"
  private val phoneNumber2 = "8 340 033 92 23"

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

      val account = ClusterSharding(testKit.system).entityRefFor(AccountActor.typeKey,
        AccountActor.entityId(phoneNumber))
      val account2 = ClusterSharding(testKit.system).entityRefFor(AccountActor.typeKey,
        AccountActor.entityId(phoneNumber))
      val probe = testKit.createTestProbe[AccountActor.State]()

      account ! AccountActor.SetPricingPlan(pricingPlanName, pricingPlan)
      account2 ! AccountActor.Get(probe.ref)

      val actual = probe.receiveMessage()

      actual.pricingPlanName should ===(pricingPlanName)
      actual.pricingPlan should ===(pricingPlan)
    }
  }
}
