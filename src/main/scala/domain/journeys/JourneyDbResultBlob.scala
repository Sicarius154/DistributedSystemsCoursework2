package domain.journeys

import cats.data.NonEmptyList

case class JourneyDbResultBlob(
    routes: NonEmptyList[Route],
    meanJourneyTime: Int,
    includesNoChangeRoute: Boolean
)