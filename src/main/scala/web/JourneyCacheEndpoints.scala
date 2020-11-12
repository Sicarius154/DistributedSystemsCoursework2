package web

import java.util.UUID

import cats.Traverse.ops.toAllTraverseOps
import domain.{JourneyID, Postcode}
import io.circe.{Encoder, Decoder}
import io.finch.circe._
import io.finch._
import io.finch.catsEffect.{jsonBody, post, get}
import io.finch.{NoContent, Endpoint, Ok}
import cats.effect.IO
import domain.journeys.{JourneyCache, Journey, Line, Route}
import domain.searches.{SearchRepository, Search}
import io.finch.catsEffect._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.finch.circe._
import cats.syntax._
import cats._
import cats.implicits._
import org.slf4j.{LoggerFactory, Logger}
import cats.Applicative._

object JourneyCacheEndpoints {
  private val log: Logger = LoggerFactory.getLogger("JourneyEndpoints")

  private implicit val searchDecoder: Decoder[Search] =
    deriveDecoder
  private implicit val searchEncoder: Encoder[Search] =
    deriveEncoder
  private implicit val journeyDecoder: Decoder[Journey] =
    deriveDecoder
  private implicit val journeyEncoder: Encoder[Journey] =
    deriveEncoder
  private implicit val historyDecoder: Decoder[UserHistory] =
    deriveDecoder
  private implicit val historyEncoder: Encoder[UserHistory] =
    deriveEncoder

  private implicit val routeDecoder: Decoder[Route] =
    deriveDecoder
  private implicit val routeEncoder: Encoder[Route] =
    deriveEncoder

  private implicit val lineDecoder: Decoder[Line] =
    deriveDecoder
  private implicit val lineEncoder: Encoder[Line] =
    deriveEncoder

  private implicit val insertJourneyRequestDecoder
      : Decoder[InsertJourneyRequest] =
    deriveDecoder
  private implicit val insertJourneyRequestEncoder
      : Encoder[InsertJourneyRequest] =
    deriveEncoder

  def getJourney(repository: JourneyCache): Endpoint[IO, Journey] =
    get("journey" :: param[String]("start") :: param[String]("end")) {
      (start: Postcode, end: Postcode) =>
        log.info(s"GET Request received ")
        for {
          journey <- repository.getJourneyByPostcodes(start, end)
          res = journey match {
            case Some(journey) => Ok(journey)
            case None          => NoContent
          }
        } yield res
    }

  def getJourneyHistory(
      searchRepository: SearchRepository,
      journeyCache: JourneyCache
  ): Endpoint[IO, UserHistory] =
    get("history" :: header[String]("jwt")) { token: String =>
      val userID: String = "11ff5ee5-65c7-4ccd-826d-ad9e57238adb"
      log.info(s"GET Request received for user journey history $userID")

      val userSearches: IO[List[JourneyID]] =
        searchRepository
          .getUserSearches(UUID.fromString(userID))
          .map(searches => searches.map(_.journeyID))

      for {
        userSearches <- userSearches.map { items: List[JourneyID] =>
          items.map { item =>
            journeyCache.getJourneyByJourneyID(item).unsafeRunSync() //TODO: This is unsafe! need to find a way around this!
          }
        }

      } yield Ok(UserHistory(userSearches))
    }

  def insertJourney(repository: JourneyCache): Endpoint[IO, String] =
    post("journey" :: jsonBody[InsertJourneyRequest]) {
      insertJourneyRequest: InsertJourneyRequest =>
        NoContent[String]
    }
}
