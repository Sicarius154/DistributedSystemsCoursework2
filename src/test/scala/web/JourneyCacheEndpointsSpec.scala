package web
import java.nio.charset.StandardCharsets
import java.util.UUID

import cats.data.NonEmptyList
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
import scala.io.Source

class JourneyCacheEndpointsSpec
    extends AnyWordSpec
    with Matchers
    with Eventually
    with BeforeAndAfterAll {
  "/journey" should {
    "Return 200 OK" when {
      "A valid start and end postcode are submitted" in {
        TestSupport.withHardcodedJourneyCache() {
          cache: HardcodedJourneyCache =>
            val req = Input
              .get(
                JourneyCacheEndpointsSpec.getJourneyEndpoint,
                "start" -> "ME7 2EJ",
                "end" -> "SW1A VC1"
              )
              .withHeaders(
                "jwt" -> JourneyCacheEndpointsSpec.validJwtToken
              )

            eventually {
              JourneyCacheEndpoints
                .getJourney(
                  cache,
                  JourneyCacheEndpointsSpec.jwtSecret,
                  JourneyCacheEndpointsSpec.jwtAlgorithm
                )(req)
                .awaitOutputUnsafe()
                .map(_.status) mustBe Some(Status.Ok)
            }
        }
      }
    }

    "Return 406 NotAcceptable" when {
      "an invalid JWT token is supplied" in {
        TestSupport.withHardcodedJourneyCache() {
          cache: HardcodedJourneyCache =>
            val req = Input
              .get(
                JourneyCacheEndpointsSpec.getJourneyEndpoint,
                "start" -> "ME7 2EJ",
                "end" -> "SW1A VC1"
              )
              .withHeaders(
                "jwt" -> JourneyCacheEndpointsSpec.invalidJwtToken
              )

            eventually {
              JourneyCacheEndpoints
                .getJourney(
                  cache,
                  JourneyCacheEndpointsSpec.jwtSecret,
                  JourneyCacheEndpointsSpec.jwtAlgorithm
                )(req)
                .awaitOutputUnsafe()
                .map(_.status) mustBe Some(Status.NotAcceptable)
            }
        }
      }
    }

    "Return the correct decoded JSON for a Journey" when {
      "when provided with valid postcodes" in {
        TestSupport.withHardcodedJourneyCache() {
          cache: HardcodedJourneyCache =>
            val req = Input
              .get(
                JourneyCacheEndpointsSpec.getJourneyEndpoint,
                "start" -> "ME7 2EJ",
                "end" -> "SW1A VC1"
              )
              .withHeaders(
                "jwt" -> JourneyCacheEndpointsSpec.validJwtToken
              )

            eventually {
              JourneyCacheEndpoints
                .getJourney(
                  cache,
                  JourneyCacheEndpointsSpec.jwtSecret,
                  JourneyCacheEndpointsSpec.jwtAlgorithm
                )(req)
                .awaitOutputUnsafe()
                .map(
                  _.value
                ) mustEqual JourneyCacheEndpointsSpec.validPostcodesResult
            }
        }
      }
    }
  }

  "/history" should {
    "Return the correct user history" when {
      "a valid JWT token is supplied and the user ID is correct" in {
        TestSupport.withHardcodedJourneyCache() {
          cache: HardcodedJourneyCache =>
            val req = Input
              .get(JourneyCacheEndpointsSpec.getHistoryEndpoint)
              .withHeaders(
                "jwt" -> JourneyCacheEndpointsSpec.validJwtToken
              )

            eventually {
              JourneyCacheEndpoints
                .getJourney(
                  cache,
                  JourneyCacheEndpointsSpec.jwtSecret,
                  JourneyCacheEndpointsSpec.jwtAlgorithm
                )(req)
                .awaitOutputUnsafe()
                .map(
                  _.value
                ) mustEqual JourneyCacheEndpointsSpec.validUserHistory //TODO: Maybe just use sameElementsAs
            }
        }

      }
    }
  }

}

object JourneyCacheEndpointsSpec {
  val appConfig: Config = TestSupport.loadConfig
  val jwtSecret: String = appConfig.jwtConfig.secret
  val jwtAlgorithm: String = appConfig.jwtConfig.algorithm

  val validJwtToken: String =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyTmFtZSI6IkNocmlzIiwiaWQiOiIxMWZmNWVlNS02NWM3LTRjY2QtODI2ZC1hZDllNTcyMzhhZGIifQ.qT15VVzzodKVZqcYsI-x2dzkWDnb_KiyRFhRYyY3PPE"

  val invalidJwtToken: String =
    "ayJhbGciOiJIUzH1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyTmFtZSI6IkNocmlzIiwiaWQiOiIxMWZmNWVlNS02NWM3LTRjY2QtODI2ZC1hZDllNTcyMzhhZGIifQ.qT15VVzzodKVZqcYsI-x2dzkWDnb_KiyRFhRYyY3PPE"

  val validPostcodesResult: Journey = Journey(
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
  )

  val validUserHistory: List[Journey] = List[Journey](
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
    )
  )

  val getJourneyEndpoint: String = "/journey"

  val getHistoryEndpoint: String = "/history"

}
