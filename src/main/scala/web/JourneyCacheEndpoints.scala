package web

import java.util.UUID

import domain.Postcode
import io.finch.circe._
import io.finch._
import io.finch.catsEffect.{jsonBody, post, get}
import io.finch.{NoContent, Endpoint, Ok}
import cats.effect.IO
import domain.journeys.{JourneyCache, Journey}
import io.finch.catsEffect._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.finch.circe._
import cats.syntax._
import cats._
import cats.implicits._
import web._
import org.slf4j.{LoggerFactory, Logger}

object JourneyCacheEndpoints {
  private val log: Logger = LoggerFactory.getLogger("JourneyEndpoints")

  def getJourney(
      repository: JourneyCache,
      jwtSecret: String,
      jwtAlgorithm: String
  ): Endpoint[IO, Journey] =
    get(
      "journey" :: param[String]("start") :: param[String]("end") :: header[String]("jwt")) {
      (start: Postcode, end: Postcode, token: String) =>
      log.info(s"GET Request received ")
      Support.decodeJwtToken(token, jwtSecret, jwtAlgorithm) match {
        case Right(tokenResult) => {
          for {
            journey <- repository.getJourneyByPostcodes(start, end)
            res = journey match {
              case Some(journey) => Ok(journey)
              case None          => NoContent
            }
          } yield res
        }
        case Left(err) => {
          log.error(s"Error decoding JWT token. Returning HTTP 406")
          IO(NotAcceptable(new Exception(err)))
        }
      }
    }

  def insertJourney(
      repository: JourneyCache,
      jwtSecret: String,
      jwtAlgorithm: String
  ): Endpoint[IO, String] =
    post("journey" :: jsonBody[InsertJourneyRequest]) {
      insertJourneyRequest: InsertJourneyRequest =>
        NoContent[String]
    }
}
