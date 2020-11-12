package domain

import cats.data.NonEmptyList
import cats.effect.IO


trait JourneyCache {
  def getJourneyByPostcodes(start: Postcode, end: Postcode): IO[Option[Journey]]
  def insertJourney(journey: Journey): IO[Unit]
}
