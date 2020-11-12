package web

import cats.effect.IO
import domain.journeys.{JourneyCache, Journey}
import domain.searches.SearchRepository
import io.finch.Endpoint
import shapeless.{CNil, :+:}

object Endpoints {
  def journeyCacheEndpoints(
      journeyCache: JourneyCache,
      searchRepository: SearchRepository
  ): Endpoint[IO, Journey :+: UserHistory :+: String :+: CNil] =
    JourneyCacheEndpoints
      .getJourney(journeyCache) :+: JourneyCacheEndpoints.getJourneyHistory(
      searchRepository,
      journeyCache
    ) :+: JourneyCacheEndpoints
      .insertJourney(journeyCache)
}
