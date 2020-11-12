package domain.journeys

import java.util.UUID

import cats.effect.IO
import cats.data.{EitherT, NonEmptyList}
import domain.{Postcode, JourneyID}
import org.slf4j.{LoggerFactory, Logger}

class HardcodedJourneyCache(implicit val logger: Logger) extends JourneyCache {
  override def getJourneyByPostcodes(
      start: Postcode,
      end: Postcode
  ): IO[Option[Journey]] =
    IO.pure(HardcodedJourneyCache.repository.filter { journey =>
      journey.start.equals(start) && journey.end.equals(end)
    }.headOption)

  override def getJourneyByJourneyID(
      journeyID: JourneyID
  ): IO[List[Journey]] =
    IO.pure(HardcodedJourneyCache.repository.filter { journey =>
      journey.journeyID.equals(journeyID)
    })

  override def insertJourney(journey: Journey): IO[Unit] = {
    logger.warn(
      "Hardcoded repository does not support insertions, returning Unit anyway..."
    )
    IO.unit
  }
}

object HardcodedJourneyCache {
  private implicit val logger: Logger =
    LoggerFactory.getLogger("HardcodedJourneyCache")

  private val repository = NonEmptyList.of(
    Journey(
      UUID.fromString("3bde7cb0-c1dd-42ca-b1d6-a5e2d5662ef1"),
      "ME7 2EJ",
      "SW1A VC1",
      NonEmptyList
        .of(
          Route(NonEmptyList.of(Line("Victoria"), Line("Jubilee")), 24),
          Route(NonEmptyList.of(Line("Victoria"), Line("Central")), 26)
        ),
      25,
      includesNoChangeRoute = false
    ),
    Journey(
      UUID.fromString("a55eb972-7c5d-43b4-8a33-6be2fb371dba"),
      "E14 9UY",
      "E1, 5JT",
      NonEmptyList
        .of(
          Route(
            NonEmptyList.of(Line("DLR"), Line("District"), Line("Northern")),
            36
          ),
          Route(NonEmptyList.of(Line("DLR")), 20)
        ),
      27,
      includesNoChangeRoute = true
    ),
    Journey(
      UUID.fromString("c7f673a8-8d5d-4941-8575-13aeed66b59f"),
      "SE8 5JT",
      "SW1A Y6T",
      NonEmptyList
        .of(
          Route(NonEmptyList.of(Line("Jubilee"), Line("Central")), 24),
          Route(NonEmptyList.of(Line("Circle"), Line("DLR")), 36)
        ),
      29,
      includesNoChangeRoute = false
    )
  )

  def apply(): EitherT[IO, String, HardcodedJourneyCache] =
    EitherT.right(IO(new HardcodedJourneyCache()))
}
