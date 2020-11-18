package web

import cats.Parallel
import cats.effect.IO
import domain.journeys.{JourneyCache, Journey}
import domain.searches.SearchRepository
import io.finch.Endpoint
import shapeless.{CNil, :+:}

object Endpoints {
  //Combine all endpoints
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
  ): Endpoint[IO, Journey :+: String :+: CNil] = {
    val endpoints = new JourneyCacheEndpoints(journeyCache, searchRepository)

    endpoints
      .getJourney(
        jwtSecret,
        jwtAlgorithm
      ) :+: endpoints
      .insertJourney(jwtSecret, jwtAlgorithm)
  }
  def userJourneyHistoryEndpoints(
      journeyCache: JourneyCache,
      searchRepository: SearchRepository,
      jwtSecret: String,
      jwtAlgorithm: String
  ): Endpoint[IO, UserHistory] = {
    val endpoints =
      new UserJourneyHistoryEndpoints(journeyCache, searchRepository)
    endpoints.getJourneyHistory(
      jwtSecret,
      jwtAlgorithm
    )
  }
}
