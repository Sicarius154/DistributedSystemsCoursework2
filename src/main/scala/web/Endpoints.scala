package web

import cats.effect.IO
import domain.journeys
import domain.journeys.{Journey, JourneyCache}
import io.finch.Endpoint
import shapeless.{CNil, :+:}

object Endpoints {
  def journeyCacheEndpoints(
      cache: JourneyCache
  ): Endpoint[IO, Journey :+: String :+: CNil] =
    JourneyCacheEndpoints.getJourney(cache) :+: JourneyCacheEndpoints
      .insertJourney(cache)
}
