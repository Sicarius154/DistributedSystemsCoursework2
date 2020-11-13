package web

import domain.journeys.{Journey, Route}
import domain.{UserID, Postcode}

case class InsertJourneyRequest(start: Postcode, end: Postcode, routes: List[Route])

