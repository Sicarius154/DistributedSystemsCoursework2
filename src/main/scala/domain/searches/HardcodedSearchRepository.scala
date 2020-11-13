package domain.searches

import cats.effect.IO
import cats.data.{EitherT, NonEmptyList, Nested}
import domain.{JourneyID, UserID}
import org.slf4j.{LoggerFactory, Logger}

class HardcodedSearchRepository(implicit val logger: Logger)
    extends SearchRepository {

  //TODO: Consider using OptionT[IO, List[Search]]
  override def getUserSearches(userID: UserID): Nested[IO, List, Search] =
    Nested(IO(HardcodedSearchRepository.repository.filter { search =>
      search.userID.equals(userID)
    }))

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
        "f31c9cab-2f49-4040-931a-8c82600a903f",
        "3bde7cb0-c1dd-42ca-b1d6-a5e2d5662ef1"
      ),
      Search(
        "613ae23f-caad-438e-a5ea-d8bd50fbe91d",
        "a55eb972-7c5d-43b4-8a33-6be2fb371dba"
      ),
      Search(
        "11ff5ee5-65c7-4ccd-826d-ad9e57238adb",
        "a55eb972-7c5d-43b4-8a33-6be2fb371dba"
      ),
      Search(
        "11ff5ee5-65c7-4ccd-826d-ad9e57238adb",
        "3bde7cb0-c1dd-42ca-b1d6-a5e2d5662ef1"
      )
    )

  def apply(): EitherT[IO, String, HardcodedSearchRepository] =
    EitherT.right(IO(new HardcodedSearchRepository()))
}
