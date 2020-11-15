package domain.journeys

case class JourneyDbResultBlob(
    routes: List[Route],
    meanJourneyTime: Int,
    includesNoChangeRoute: Boolean
)