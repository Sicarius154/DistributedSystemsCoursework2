package domain.journeys

import cats.data.NonEmptyList

case class Route(lines: NonEmptyList[Line], journeyTime: Int)
