package web

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
  ): Endpoint[IO, Journey :+: String :+: UserHistory :+: CNil] =
    journeyCacheEndpoints(
      journeyCache,
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
      jwtSecret: String,
      jwtAlgorithm: String
  ): Endpoint[IO, Journey :+: String :+: CNil] =
    JourneyCacheEndpoints
      .getJourney(
        journeyCache,
        jwtSecret,
        jwtAlgorithm
      ) :+: JourneyCacheEndpoints
      .insertJourney(journeyCache, jwtSecret, jwtAlgorithm)

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
