package domain.journeys

import cats.data.OptionT
import cats.effect.IO
import domain.{JourneyID, Postcode}

trait JourneyCache {
  def getJourneyByPostcodes(start: Postcode, end: Postcode): OptionT[IO, Journey]
  def getJourneyByJourneyID(journeyID: JourneyID): OptionT[IO, Journey]
  def insertJourney(journey: Journey): IO[Unit]
}
