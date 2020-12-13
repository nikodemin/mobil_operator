package com.github.nikodemin.mobileoperator.query.route.util

import com.github.nikodemin.mobileoperator.query.model.dto._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

object Implicits {
  implicit val accountResponseDtoDecoder: Decoder[AccountResponseDto] = deriveDecoder[AccountResponseDto]
  implicit val accountResponseDtoEncoder: Encoder[AccountResponseDto] = deriveEncoder[AccountResponseDto]
  implicit val accountQueryDtoDecoder: Decoder[AccountQueryDto] = deriveDecoder[AccountQueryDto]
  implicit val accountQueryDtoEncoder: Encoder[AccountQueryDto] = deriveEncoder[AccountQueryDto]
  implicit val accountGetByLastTakeOffDateDecoder: Decoder[AccountGetByLastTakeOffDate] =
    deriveDecoder[AccountGetByLastTakeOffDate]
  implicit val accountGetByLastTakeOffDateEncoder: Encoder[AccountGetByLastTakeOffDate] =
    deriveEncoder[AccountGetByLastTakeOffDate]
  implicit val userQueryDtoDecoder: Decoder[UserQueryDto] = deriveDecoder[UserQueryDto]
  implicit val userQueryDtoEncoder: Encoder[UserQueryDto] = deriveEncoder[UserQueryDto]
  implicit val userResponseDtoDecoder: Decoder[UserResponseDto] = deriveDecoder[UserResponseDto]
  implicit val userResponseDtoEncoder: Encoder[UserResponseDto] = deriveEncoder[UserResponseDto]
}
