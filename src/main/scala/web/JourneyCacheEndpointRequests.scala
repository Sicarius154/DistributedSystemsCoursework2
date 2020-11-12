package web

import domain.journeys.Journey
import domain.UserID

case class InsertJourneyRequest(journey: Journey, userID: UserID)

case class UserHistory(journeys: Seq[Journey])