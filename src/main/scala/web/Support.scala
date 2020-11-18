package web
import pdi.jwt.{JwtAlgorithm, JwtCirce}
import domain.UserID
import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class TokenResult(id: UserID, username: String)

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
    //TODO: Look into using alternate algorithms
    val jwtAlgorithm = algorithm match {
      case "HS256" => JwtAlgorithm.HS256
      case _ => JwtAlgorithm.HS256 //Default to HS256
    }

    JwtCirce
      .decodeJson(token, secret, Seq(jwtAlgorithm))
      .toEither //We want to remove the Try instance and use Either
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
