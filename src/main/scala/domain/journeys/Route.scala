package domain.journeys

import cats.data.NonEmptyList

case class Route(orderedLines: NonEmptyList[Line], journeyTime: Int)
