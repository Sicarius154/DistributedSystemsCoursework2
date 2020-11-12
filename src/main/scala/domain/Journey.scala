package domain

import cats.data.NonEmptyList

case class Journey (journeyID: JourneyID, start: Postcode, end: Postcode, routes: NonEmptyList[Route], meanJourneyTime: Int, includesNoChangeRoute: Boolean)
