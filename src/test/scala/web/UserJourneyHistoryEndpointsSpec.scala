package web
import java.nio.charset.StandardCharsets
import java.util.UUID

import cats.Parallel
import cats.data.NonEmptyList
import cats.effect.{IO, ContextShift}
import com.twitter.finagle.http.Status
import config.Config
import domain.journeys.{Journey, Line, HardcodedJourneyCache, Route}
import io.finch.Input
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.concurrent.Eventually
import test.TestSupport
import io.circe.syntax._
import domain.journeys._
import domain.searches.HardcodedSearchRepository

import scala.concurrent.ExecutionContext
import scala.io.Source

class UserJourneyHistoryEndpointsSpec
    extends AnyWordSpec
    with Matchers
    with Eventually
    with BeforeAndAfterAll {
  private implicit val ec: ExecutionContext = ExecutionContext.global
  private implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  private implicit val parallel: Parallel[IO] = IO.ioParallel
  "/history" should {
    "Return the correct user history" when {
      "a valid JWT token is supplied and the user ID is correct" in {
        TestSupport.withHardcodedJourneyCache() {
          cache: HardcodedJourneyCache =>
            TestSupport.withHardcodedSearchRepository() {
              searchRepo: HardcodedSearchRepository =>
                val req = Input
                  .get(UserJourneyHistoryEndpointsSpec.getHistoryEndpoint)
                  .withHeaders(
                    "jwt" -> UserJourneyHistoryEndpointsSpec.validJwtToken
                  )

                eventually {
                  new JourneyCacheEndpoints(cache, searchRepo)
                    .getJourney(
                      JourneyCacheEndpointsSpec.jwtSecret,
                      JourneyCacheEndpointsSpec.jwtAlgorithm
                    )(parallel)(req)
                    .awaitOutputUnsafe()
                    .map(
                      _.value
                    ) mustEqual UserJourneyHistoryEndpointsSpec.validUserHistory //TODO: Maybe just use sameElementsAs
                }
            }
        }
      }
    }
  }

}

object UserJourneyHistoryEndpointsSpec {
  val appConfig: Config = TestSupport.loadConfig
  val jwtSecret: String = appConfig.jwtConfig.secret
  val jwtAlgorithm: String = appConfig.jwtConfig.algorithm

  val validJwtToken: String =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyTmFtZSI6IkNocmlzIiwiaWQiOiIxMWZmNWVlNS02NWM3LTRjY2QtODI2ZC1hZDllNTcyMzhhZGIifQ.qT15VVzzodKVZqcYsI-x2dzkWDnb_KiyRFhRYyY3PPE"

  val invalidJwtToken: String =
    "ayJhbGciOiJIUzH1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyTmFtZSI6IkNocmlzIiwiaWQiOiIxMWZmNWVlNS02NWM3LTRjY2QtODI2ZC1hZDllNTcyMzhhZGIifQ.qT15VVzzodKVZqcYsI-x2dzkWDnb_KiyRFhRYyY3PPE"

  val validUserHistory: List[Journey] = List[Journey](
    Journey(
      "a55eb972-7c5d-43b4-8a33-6be2fb371dba",
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
      "3bde7cb0-c1dd-42ca-b1d6-a5e2d5662ef1",
      "ME7 2EJ",
      "SW1A VC1",
      NonEmptyList
        .of(
          Route(NonEmptyList.of(Line("Victoria"), Line("Jubilee")), 24),
          Route(NonEmptyList.of(Line("Victoria"), Line("Central")), 26)
        ),
      25,
      includesNoChangeRoute = false
    )
  )

  val getHistoryEndpoint: String = "/history"

}
