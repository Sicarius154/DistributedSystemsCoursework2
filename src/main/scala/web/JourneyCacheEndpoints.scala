package web

import java.util.UUID

import cats.Parallel
import cats.data.NonEmptyList
import domain.{JourneyID, Postcode, UserID}
import io.finch._
import io.finch.catsEffect.{jsonBody, post, get}
import io.finch.{NoContent, Endpoint, Ok}
import cats.effect.IO
import domain.journeys.{JourneyCache, Journey, Line, Route}
import domain.searches.{SearchRepository, HardcodedSearchRepository}
import io.finch.catsEffect._
import io.finch.circe._
import web._
import cats.implicits._
import org.slf4j.{LoggerFactory, Logger}

import scala.concurrent.ExecutionContext

object JourneyCacheEndpoints {
  private val log: Logger = LoggerFactory.getLogger("JourneyEndpoints")

  def getJourney(
      repository: JourneyCache,
      jwtSecret: String,
      jwtAlgorithm: String
  ): Endpoint[IO, Journey] =
    get(
      "journey" :: param[String]("start") :: param[String]("end") :: header[
        String
      ]("jwt")
    ) { (start: Postcode, end: Postcode, token: String) =>
      log.info(s"GET Request received ")
      Support.decodeJwtToken(token, jwtSecret, jwtAlgorithm) match {
        case Right(tokenResult) => {
          for {
            journey <- repository.getJourneyByPostcodes(start, end).value
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
      cache: JourneyCache,
      searchRepository: SearchRepository,
      jwtSecret: String,
      jwtAlgorithm: String
  )(implicit parallel: Parallel[IO]): Endpoint[IO, String] =
    post("journey" :: jsonBody[InsertJourneyRequest] :: header[String]("jwt")) {
      (insertJourneyRequest: InsertJourneyRequest, token: String) =>
        Support.decodeJwtToken(token, jwtSecret, jwtAlgorithm) match {
          case Right(tokenResult) => {
            log.info("Validating new Journey")
            val validatedJourney = createJourneyFromInput(insertJourneyRequest)
            writeNewJourney(
              cache,
              searchRepository,
              tokenResult.id,
              validatedJourney
            )
          }
          case Left(err) => {
            log.error(s"Error decoding JWT token. Returning HTTP 406")
            IO(NotAcceptable(new Exception(err)))
          }
        }

    }

  private def writeNewJourney(
      cache: JourneyCache,
      searchRepository: SearchRepository,
      userID: UserID,
      validatedJourney: Option[Journey]
  )(implicit parallel: Parallel[IO]): IO[Output[Postcode]] = {
    validatedJourney match {
      case Some(journey) => {
        log.info("Writing new Journey")
        for {
          _ <- List[IO[Unit]](
            cache.insertJourney(journey),
            searchRepository.addUserSearch(
              userID,
              journey.journeyID
            )
          ).parSequence
        } yield Ok("OK!")
      }
      case None => {
        log.error(s"Error creating Journey")
        IO(NotAcceptable(new Exception()))
      }
    }

  }

  private def createJourneyFromInput(
      insertJourneyRequest: InsertJourneyRequest
  ): Option[Journey] = {
    val journeyUUID: JourneyID = UUID.randomUUID().toString

    //TODO: Look into using Monocle for this
    val lineNamesFiltered: List[(Option[NonEmptyList[Line]], Int)] =
      insertJourneyRequest.routes.map(route =>
        (
          NonEmptyList.fromList(route.lines.filterNot(_.name.equals(""))),
          route.journeyTime
        )
      )

    val routes: List[Route] = lineNamesFiltered
      .filter(input => input._1.isDefined)
      .map(input =>
        Route(input._1.get, input._2)
      ) //TODO: Clean this up to remove .get()

    val meanJourneyTime =
      routes.map(_.journeyTime).sum / routes.length

    val includesNoChangeRoute = routes.exists(_.lines.length == 1)

    if (routes.nonEmpty)
      Some(
        Journey(
          journeyUUID,
          insertJourneyRequest.start,
          insertJourneyRequest.end,
          NonEmptyList.fromList(routes).get,
          meanJourneyTime,
          includesNoChangeRoute
        )
      )
    else None
  }
}
