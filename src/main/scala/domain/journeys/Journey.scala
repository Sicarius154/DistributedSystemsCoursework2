package domain.journeys

import cats.data.NonEmptyList
import domain.{JourneyID, Postcode}

case class Journey(
    journeyID: JourneyID,
    start: Postcode,
    end: Postcode,
    routes: NonEmptyList[Route],
    meanJourneyTime: Int,
    includesNoChangeRoute: Boolean
)
