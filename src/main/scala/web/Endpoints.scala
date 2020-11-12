package web

import cats.effect.IO
import domain.journeys.{JourneyCache, Journey}
import domain.searches.SearchRepository
import io.finch.Endpoint
import shapeless.{CNil, :+:}

object Endpoints {
  def journeyCacheEndpoints(
      journeyCache: JourneyCache,
      searchRepository: SearchRepository,
      jwtSecret: String,
      jwtAlgorithm: String
  ): Endpoint[IO, Journey :+: UserHistory :+: String :+: CNil] =
    JourneyCacheEndpoints
      .getJourney(
        journeyCache,
        jwtSecret,
        jwtAlgorithm
      ) :+: JourneyCacheEndpoints.getJourneyHistory(
      searchRepository,
      journeyCache,
      jwtSecret,
      jwtAlgorithm
    ) :+: JourneyCacheEndpoints
      .insertJourney(journeyCache, jwtSecret, jwtAlgorithm)
}
