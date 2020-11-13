package web

import domain.{JourneyID, UserID}
import cats.implicits._
import io.finch._
import io.finch.catsEffect.get
import io.finch.{Endpoint, Ok}
import cats.effect.IO
import domain.journeys.{JourneyCache, Journey}
import domain.searches.SearchRepository
import io.finch.catsEffect._
import web._
import org.slf4j.{LoggerFactory, Logger}

object UserJourneyHistoryEndpoints {
  private val log: Logger = LoggerFactory.getLogger("HistoryEndpoints")

  //TODO: Tidy this function up
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
            getUserSearchHistory(tokenResult.id, searchRepository)

          for {
            history <- userSearchHistoryToJourneys(userSearches, journeyCache)
          } yield Ok(history)
        }
        case Left(err) => {
          log.error(s"Error decoding JWT token. Returning HTTP 406")
          IO(NotAcceptable(new Exception(err)))
        }
      }
    }

  //TODO: The following works and is correct, but I'm sure this can be cleaner
  private def userSearchHistoryToJourneys(
      userSearches: IO[List[JourneyID]],
      journeyCache: JourneyCache
  ): IO[UserHistory] =
    for {
      userSearches <- userSearches.map(
        _.map(journeyCache.getJourneyByJourneyID(_).value)
      )
      historyFromDatabase <- userSearches.sequence
      history = historyFromDatabase.filter(_.isDefined).map(_.get)
    } yield UserHistory(history)

  private def getUserSearchHistory(
      userID: UserID,
      searchRepository: SearchRepository
  ): IO[List[JourneyID]] =
    searchRepository
      .getUserSearches(userID)
      .value //TODO: Remove this, otherwise, why use Nested?
      .map(searches => searches.map(_.journeyID))
}
