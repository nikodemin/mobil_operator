package com.github.nikodemin.mobileoperator.cmd.route.util

import com.github.nikodemin.mobileoperator.cmd.model.dto._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

object Implicits {
  implicit val userAddDtoDecoder: Decoder[UserAddDto] = deriveDecoder[UserAddDto]
  implicit val userAddDtoEncoder: Encoder[UserAddDto] = deriveEncoder[UserAddDto]
  implicit val userResponseDtoDecoder: Decoder[UserResponseDto] = deriveDecoder[UserResponseDto]
  implicit val userResponseDtoEncoder: Encoder[UserResponseDto] = deriveEncoder[UserResponseDto]
  implicit val userChangeDtoDecoder: Decoder[UserChangeDto] = deriveDecoder[UserChangeDto]
  implicit val userChangeDtoEncoder: Encoder[UserChangeDto] = deriveEncoder[UserChangeDto]
  implicit val accountAddDtoDecoder: Decoder[AccountAddDto] = deriveDecoder[AccountAddDto]
  implicit val accountAddDtoEncoder: Encoder[AccountAddDto] = deriveEncoder[AccountAddDto]
  implicit val accountResponseDtoDecoder: Decoder[AccountResponseDto] = deriveDecoder[AccountResponseDto]
  implicit val accountResponseDtoEncoder: Encoder[AccountResponseDto] = deriveEncoder[AccountResponseDto]
  implicit val setPricingPlanDtoDecoder: Decoder[SetPricingPlanDto] = deriveDecoder[SetPricingPlanDto]
  implicit val setPricingPlanDtoEncoder: Encoder[SetPricingPlanDto] = deriveEncoder[SetPricingPlanDto]
}
