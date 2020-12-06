package com.github.nikodemin.mobileoperator.route.util

import com.github.nikodemin.mobileoperator.model.dto.{AddAccountDto, AddUserDto}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import scala.concurrent.{ExecutionContext, Future}

object Implicits {
  implicit val addAccountDtoDecoder: Decoder[AddAccountDto] = deriveDecoder[AddAccountDto]
  implicit val addAccountDtoEncoder: Encoder[AddAccountDto] = deriveEncoder[AddAccountDto]
  implicit val addUserDtoDecoder: Decoder[AddUserDto] = deriveDecoder[AddUserDto]
  implicit val addUserDtoEncoder: Encoder[AddUserDto] = deriveEncoder[AddUserDto]

  implicit def toEither[T](future: Future[T])(implicit executionContext: ExecutionContext): Future[Right[Nothing, T]] =
    future.map(Right(_))

  implicit def optionToEither[T](future: Future[Option[T]])(implicit executionContext: ExecutionContext): Future[Either[Unit, T]] =
    future.map(optionToEitherInternal)

  private def optionToEitherInternal[T](option: Option[T]) = option match {
    case Some(value) => Right(value)
    case None => Left(())
  }
}
