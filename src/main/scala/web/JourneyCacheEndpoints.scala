package web

import domain.{Journey, Route, Line, Postcode, JourneyCache}
import io.circe.{Encoder, Decoder}
import io.finch.circe._
import io.finch._
import io.finch.catsEffect.{jsonBody, post, get}
import io.finch.{NoContent, Endpoint, Ok}
import cats.effect.IO
import io.finch.catsEffect._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.finch.circe._
import org.slf4j.{LoggerFactory, Logger}

object JourneyCacheEndpoints {
  private val log: Logger = LoggerFactory.getLogger("JourneyEndpoints")

  private implicit val journeyDecoder: Decoder[Journey] =
    deriveDecoder
  private implicit val journeyEncoder: Encoder[Journey] =
    deriveEncoder

  private implicit val routeDecoder: Decoder[Route] =
    deriveDecoder
  private implicit val routeEncoder: Encoder[Route] =
    deriveEncoder

  private implicit val lineDecoder: Decoder[Line] =
    deriveDecoder
  private implicit val lineEncoder: Encoder[Line] =
    deriveEncoder

  private implicit val insertJourneyRequestDecoder: Decoder[InsertJourneyRequest] =
    deriveDecoder
  private implicit val insertJourneyRequestEncoder: Encoder[InsertJourneyRequest] =
    deriveEncoder

  def getJourney(repository: JourneyCache): Endpoint[IO, Journey] =
    get("journey" :: param[String]("start") :: param[String]("end")) { (start: Postcode, end: Postcode) =>
      log.info(s"GET Request received ")
      for {
        journey <- repository.getJourneyByPostcodes(start, end)
        res = journey match {
          case Some(journey) => Ok(journey)
          case None       => NoContent
        }
      } yield res
    }

  def insertJourney(repository: JourneyCache): Endpoint[IO, String] =
    post("journey" :: jsonBody[InsertJourneyRequest]) {
      insertJourneyRequest: InsertJourneyRequest =>
        NoContent[String]
    }
}
