package domain.journeys

import cats.effect.IO
import domain.Postcode

trait JourneyCache {
  def getJourneyByPostcodes(start: Postcode, end: Postcode): IO[Option[Journey]]
  def insertJourney(journey: Journey): IO[Unit]
}
