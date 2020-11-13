package web

import cats.Parallel
import cats.effect.IO
import domain.journeys.{JourneyCache, Journey}
import domain.searches.SearchRepository
import io.finch.Endpoint
import shapeless.{CNil, :+:}

object Endpoints {
  def all(
      journeyCache: JourneyCache,
      searchRepository: SearchRepository,
      jwtSecret: String,
      jwtAlgorithm: String
  )(implicit
      parallel: Parallel[IO]
  ): Endpoint[IO, Journey :+: String :+: UserHistory :+: CNil] =
    journeyCacheEndpoints(
      journeyCache,
      searchRepository,
      jwtSecret,
      jwtAlgorithm
    ) :+: userJourneyHistoryEndpoints(
      journeyCache,
      searchRepository,
      jwtSecret,
      jwtAlgorithm
    )

  def journeyCacheEndpoints(
      journeyCache: JourneyCache,
      searchRepository: SearchRepository,
      jwtSecret: String,
      jwtAlgorithm: String
  )(implicit
      parallel: Parallel[IO]
  ): Endpoint[IO, Journey :+: String :+: CNil] =
    JourneyCacheEndpoints
      .getJourney(
        journeyCache,
        jwtSecret,
        jwtAlgorithm
      ) :+: JourneyCacheEndpoints
      .insertJourney(journeyCache, searchRepository, jwtSecret, jwtAlgorithm)

  def userJourneyHistoryEndpoints(
      journeyCache: JourneyCache,
      searchRepository: SearchRepository,
      jwtSecret: String,
      jwtAlgorithm: String
  ): Endpoint[IO, UserHistory] =
    UserJourneyHistoryEndpoints.getJourneyHistory(
      searchRepository,
      journeyCache,
      jwtSecret,
      jwtAlgorithm
    )
}
