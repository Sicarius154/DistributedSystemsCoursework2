package web

import java.util.UUID

import domain.JourneyID
import io.finch.circe._
import io.finch._
import io.finch.catsEffect.get
import io.finch.{Endpoint, Ok}
import cats.effect.IO
import domain.journeys.JourneyCache
import domain.searches.SearchRepository
import io.finch.catsEffect._
import web._
import org.slf4j.{LoggerFactory, Logger}

object UserJourneyHistoryEndpoints {
  private val log: Logger = LoggerFactory.getLogger("HistoryEndpoints")

  def getJourneyHistory(
      searchRepository: SearchRepository,
      journeyCache: JourneyCache,
      jwtSecret: String,
      jwtAlgorithm: String
  ): Endpoint[IO, UserHistory] =
    get("history" :: header[String]("jwt")) { token: String =>
      log.info(s"GET Request received for user journey history")

      Support.decodeJwtToken(token, jwtSecret, jwtAlgorithm) match {
        case Right(tokenResult) => {
          val userID: String = tokenResult.id.toString
          log.info(s"Decoded JWT token and obtained user ID $userID")

          val userSearches: IO[List[JourneyID]] =
            searchRepository
              .getUserSearches(UUID.fromString(userID))
              .map(searches => searches.map(_.journeyID))

          for {
            userSearches <- userSearches.map { items: List[JourneyID] =>
              items.map { item =>
                journeyCache
                  .getJourneyByJourneyID(item)
                  .unsafeRunSync() //TODO: This is unsafe! need to find a way around this!
              }
            }

          } yield Ok(UserHistory(userSearches))
        }
        case Left(err) => {
          log.error(s"Error decoding JWT token. Returning HTTP 406")
          IO(NotAcceptable(new Exception(err)))
        }
      }
    }
}
