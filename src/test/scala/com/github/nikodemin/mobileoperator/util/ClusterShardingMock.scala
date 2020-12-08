package com.github.nikodemin.mobileoperator.util

import akka.cluster.sharding.typed.scaladsl.ClusterSharding

trait ClusterShardingMock extends akka.cluster.sharding.typed.javadsl.ClusterSharding with ClusterSharding
