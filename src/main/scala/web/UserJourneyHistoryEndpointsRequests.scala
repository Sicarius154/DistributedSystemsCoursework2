package web

import domain.journeys.Journey

case class UserHistory(journeys: Seq[Journey])