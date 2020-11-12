package web
import io.circe.Json
import pdi.jwt.{JwtClaim, JwtAlgorithm, JwtCirce}
import cats.syntax._
import cats.syntax.either._
import domain.UserID
import domain.searches.Search
import io.circe.{Encoder, Decoder}
import io.finch.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import scala.util.Try
import io.circe._
import io.circe.parser._

case class TokenResult(id: UserID, userName: String)

object Support {
  private implicit val tokenDecoder: Decoder[TokenResult] =
    deriveDecoder
  private implicit val tokenEncoder: Encoder[TokenResult] =
    deriveEncoder

  def decodeJwtToken(
      token: String,
      secret: String,
      algorithm: String
  ): Either[String, TokenResult] = {
    val jwtAlgorithm = algorithm match {
      case "HS256" => JwtAlgorithm.HS256
      case _ => JwtAlgorithm.HS256
    }

    JwtCirce
      .decodeJson(token, secret, Seq(jwtAlgorithm))
      .toEither
      .map(_.as[TokenResult]) match {
      case Right(decodeRes) =>
        decodeRes.toOption match {
          case Some(res) => Right(res)
          case _ => Left(s"Error decoding token body. Are all fields present?")
        }
      case Left(_) => Left("Error decoding token")
    }
  }

}
