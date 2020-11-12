package web

import domain.{Journey, UserID}

case class InsertJourneyRequest(journey: Journey, userID: UserID)
