package com.github.nikodemin.mobileoperator.query.dao.util

import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

object Implicits {
  implicit def runDBIO[T](action: DBIO[T])(implicit db: Database): Future[T] = db.run(action)

  implicit def runAddDBIO(action: DBIO[Int])(implicit db: Database, executionContext: ExecutionContext): Future[Boolean] =
    db.run(action).map(_ > 0)

  implicit def runStreamingDBIO[T](action: StreamingDBIO[Seq[T], T])(implicit db: Database): Future[Seq[T]] =
    db.run(action)
}
