package domain.journeys

import cats.effect.IO
import domain.{Postcode, JourneyID}

trait JourneyCache {
  def getJourneyByPostcodes(start: Postcode, end: Postcode): IO[Option[Journey]]
  def getJourneyByJourneyID(journeyID: JourneyID): IO[Journey]
  def insertJourney(journey: Journey): IO[Unit]
}
