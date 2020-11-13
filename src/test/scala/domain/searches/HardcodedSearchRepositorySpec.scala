package domain.searches


import test.TestSupport.withHardcodedSearchRepository
import cats.effect.IO
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.concurrent.Eventually

class HardcodedSearchRepositorySpec
    extends AnyWordSpec
    with Matchers
    with Eventually {

  "HardcodedJourneyCache" should {
    "return a Non-empty List[Search]" when {
      "one exists in the repository for a user" in withHardcodedSearchRepository() {
        repo: HardcodedSearchRepository =>
          val res: IO[List[Search]] =
            repo
              .getUserSearches(HardcodedSearchRepositorySpec.validUserID)
              .value

          res.unsafeRunSync() mustEqual Some(
            HardcodedSearchRepositorySpec.validUserIDSearchesNonEmpty
          )
      }
    }
    "return an empty List[Search]" when {
      "no searches exist in the repository for a user" in withHardcodedSearchRepository() {
        repo: HardcodedSearchRepository =>
          val res: IO[List[Search]] =
            repo
              .getUserSearches(
                HardcodedSearchRepositorySpec.validUserIDNoResults
              )
              .value

          res.unsafeRunSync() mustEqual Some(
            HardcodedSearchRepositorySpec.validUserIDSearchesEmpty
          )
      }
    }

  }
}

object HardcodedSearchRepositorySpec {
  val validUserID: String =
    "f31c9cab-2f49-4040-931a-8c82600a903f"
  val validUserIDNoResults: String =
    "f31c9cab-2f49-4040-931a-8c82600a902f"

  val validUserIDSearchesNonEmpty: List[Search] = List[Search](
    Search(
      "f31c9cab-2f49-4040-931a-8c82600a903f",
      "3bde7cb0-c1dd-42ca-b1d6-a5e2d5662ef1"
    )
  )
  val validUserIDSearchesEmpty: List[Search] = List.empty[Search]

}
