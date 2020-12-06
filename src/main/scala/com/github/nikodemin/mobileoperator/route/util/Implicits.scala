package com.github.nikodemin.mobileoperator.route.util

import com.github.nikodemin.mobileoperator.actor.UserActor
import com.github.nikodemin.mobileoperator.model.dto.{AccountAddDto, UserAddDto, UserChangeDto, UserGetDto}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import scala.concurrent.{ExecutionContext, Future}

object Implicits {
  implicit val accountAddDtoDecoder: Decoder[AccountAddDto] = deriveDecoder[AccountAddDto]
  implicit val accountAddDtoEncoder: Encoder[AccountAddDto] = deriveEncoder[AccountAddDto]
  implicit val userAddDtoDecoder: Decoder[UserAddDto] = deriveDecoder[UserAddDto]
  implicit val userAddDtoEncoder: Encoder[UserAddDto] = deriveEncoder[UserAddDto]
  implicit val userGetDtoDecoder: Decoder[UserGetDto] = deriveDecoder[UserGetDto]
  implicit val userGetDtoEncoder: Encoder[UserGetDto] = deriveEncoder[UserGetDto]
  implicit val userChangeDtoDecoder: Decoder[UserChangeDto] = deriveDecoder[UserChangeDto]
  implicit val userChangeDtoEncoder: Encoder[UserChangeDto] = deriveEncoder[UserChangeDto]
  implicit val userChangtoDecoder: Decoder[UserActor.Event] = deriveDecoder[UserActor.Event]
  implicit val userChangtoEncoder: Encoder[UserActor.Event] = deriveEncoder[UserActor.Event]

  implicit def toEither[T](future: Future[T])(implicit executionContext: ExecutionContext): Future[Right[Nothing, T]] =
    future.map(Right(_))

  implicit def optionToEither[T](future: Future[Option[T]])(implicit executionContext: ExecutionContext): Future[Either[Unit, T]] =
    future.map(optionToEitherInternal)

  private def optionToEitherInternal[T](option: Option[T]) = option match {
    case Some(value) => Right(value)
    case None => Left(())
  }
}
