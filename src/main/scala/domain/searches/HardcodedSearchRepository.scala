package domain.searches

import java.util.UUID

import cats.effect.IO
import cats.data.{EitherT, NonEmptyList}
import domain.journeys.HardcodedJourneyCache
import domain.{JourneyID, UserID}
import org.slf4j.{LoggerFactory, Logger}

class HardcodedSearchRepository(implicit val logger: Logger)
    extends SearchRepository {
  override def getUserSearches(userID: UserID): IO[List[Search]] =
    IO.pure(HardcodedSearchRepository.repository.filter { search =>
      search.userID.equals(userID)
    })

  override def addUserSearch(userID: UserID, journeyID: JourneyID): IO[Unit] = {
    logger.warn(
      "Hardcoded repository does not support insertions, returning Unit anyway..."
    )
    IO.unit
  }
}

object HardcodedSearchRepository {
  private implicit val logger: Logger =
    LoggerFactory.getLogger("HardcodedSearchRepository")

  private val repository =
    NonEmptyList.of(
      Search(
        UUID.fromString("f31c9cab-2f49-4040-931a-8c82600a903f"),
        UUID.fromString("3bde7cb0-c1dd-42ca-b1d6-a5e2d5662ef1")
      ),
      Search(
        UUID.fromString("613ae23f-caad-438e-a5ea-d8bd50fbe91d"),
        UUID.fromString("a55eb972-7c5d-43b4-8a33-6be2fb371dba")
      ),
      Search(
        UUID.fromString("11ff5ee5-65c7-4ccd-826d-ad9e57238adb"),
        UUID.fromString("a55eb972-7c5d-43b4-8a33-6be2fb371dba")
      ),
      Search(
        UUID.fromString("81ecd117-085e-4202-80e1-9bd738ffab8e"),
        UUID.fromString("a55eb972-7c5d-43b4-8a33-6be2fb371dba")
      )
    )

  def apply(): EitherT[IO, String, HardcodedSearchRepository] =
    EitherT.right(IO(new HardcodedSearchRepository()))
}
