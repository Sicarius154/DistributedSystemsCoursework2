package domain.journeys

import java.util.UUID

import test.TestSupport.withHardcodedJourneyCache
import cats.data.NonEmptyList
import cats.effect.IO
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.concurrent.Eventually

class HardcodedJourneyCacheSpec
    extends AnyWordSpec
    with Matchers
    with Eventually {

  "HardcodedJourneyCache" should {
    "return a Journey when one exists in the repository" in
      withHardcodedJourneyCache() { cache: HardcodedJourneyCache =>
        val validStartPostCode = "E14 9UY"
        val validEndPostCode = "E1, 5JT"
        val res: IO[Option[Journey]] =
          cache.getJourneyByPostcodes(validStartPostCode, validEndPostCode).value

        res.unsafeRunSync() mustEqual Some(
          HardcodedJourneyCacheSpec.validStartEndPostcodeJourneyResult
        )
      }

  }
  "return None when a Journey does not exist in the repository" in
    withHardcodedJourneyCache() { cache: HardcodedJourneyCache =>
      val validStartPostCode = "S14 9JY"
      val validEndPostCode = "E1, 5YT"
      val res: IO[Option[Journey]] =
        cache.getJourneyByPostcodes(validStartPostCode, validEndPostCode).value

      res.unsafeRunSync() mustEqual None
    }

  "return an error when an insertion is attempted" in
    withHardcodedJourneyCache() { cache: HardcodedJourneyCache =>
      val res = cache.insertJourney(
        HardcodedJourneyCacheSpec.validStartEndPostcodeJourneyInsertion
      )

      res.unsafeRunSync() mustEqual ()
    }

}

object HardcodedJourneyCacheSpec {
  val validStartEndPostcodeJourneyResult: Journey = Journey(
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
  )

  val validStartEndPostcodeJourneyInsertion: Journey = Journey(
    UUID.randomUUID(),
    "W14 7UY",
    "E15, 9JH",
    NonEmptyList
      .of(
        Route(
          NonEmptyList.of(Line("DLR"), Line("District"), Line("Northern")),
          36
        )
      ),
    36,
    includesNoChangeRoute = false
  )

}
