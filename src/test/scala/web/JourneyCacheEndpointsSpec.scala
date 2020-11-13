package web
import java.nio.charset.StandardCharsets

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

class JourneyCacheEndpointsSpec
    extends AnyWordSpec
    with Matchers
    with Eventually
    with BeforeAndAfterAll {
  private implicit val ec: ExecutionContext = ExecutionContext.global
  private implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  private implicit val parallel: Parallel[IO] = IO.ioParallel

  "GET /journey" should {
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

  "POST /journey" should {
    "return 200 OK" when {
      "a valid message is passed with a valid JWT token" in {
        TestSupport.withHardcodedJourneyCache() { cache: HardcodedJourneyCache =>
          TestSupport.withHardcodedSearchRepository() { repo: HardcodedSearchRepository =>
            val req = Input
              .post(
                JourneyCacheEndpointsSpec.getJourneyEndpoint,
              )
              .withHeaders(
                "jwt" -> JourneyCacheEndpointsSpec.validJwtToken
              )

            TestSupport.setReqJsonBodyAndSize(req, JourneyCacheEndpointsSpec.validJourneyPostDataJson)

            eventually {
              JourneyCacheEndpoints
                .insertJourney(
                  cache,
                  repo,
                  JourneyCacheEndpointsSpec.jwtSecret,
                  JourneyCacheEndpointsSpec.jwtAlgorithm
                )(parallel)(req)
                .awaitOutputUnsafe()
                .map(_.status) mustBe Some(Status.Ok)
            }
          }
        }
      }
    }
    "return 406 NotAcceptable" when {
      "an invalid message is passed " in {
        TestSupport.withHardcodedJourneyCache() { cache: HardcodedJourneyCache =>
          TestSupport.withHardcodedSearchRepository() { repo: HardcodedSearchRepository =>
            val req = Input
              .post(
                JourneyCacheEndpointsSpec.getJourneyEndpoint,
              )
              .withHeaders(
                "jwt" -> JourneyCacheEndpointsSpec.validJwtToken
              )

            TestSupport.setReqJsonBodyAndSize(req, JourneyCacheEndpointsSpec.invalidJourneyPostDataJson)

            eventually {
              JourneyCacheEndpoints
                .insertJourney(
                  cache,
                  repo,
                  JourneyCacheEndpointsSpec.jwtSecret,
                  JourneyCacheEndpointsSpec.jwtAlgorithm
                )(parallel)(req)
                .awaitOutputUnsafe()
                .map(_.status) mustBe Some(Status.NotAcceptable)
            }
          }
        }
      }
    }

    "a valid message is passed with a valid JWT token" in {
      TestSupport.withHardcodedJourneyCache() { cache: HardcodedJourneyCache =>
        TestSupport.withHardcodedSearchRepository() { repo: HardcodedSearchRepository =>
          val req = Input
            .post(
              JourneyCacheEndpointsSpec.getJourneyEndpoint,
            )
            .withHeaders(
              "jwt" -> JourneyCacheEndpointsSpec.validJwtToken
            )

          TestSupport.setReqJsonBodyAndSize(req, JourneyCacheEndpointsSpec.validJourneyPostDataJson)

          eventually {
            JourneyCacheEndpoints
              .insertJourney(
                cache,
                repo,
                JourneyCacheEndpointsSpec.jwtSecret,
                JourneyCacheEndpointsSpec.jwtAlgorithm
              )(parallel)(req)
              .awaitOutputUnsafe()
              .map(_.status) mustBe Some(Status.Ok)
          }
        }
      }
    }
  }

    "write to the journey cache" when {
      "a valid message is passed with a valid JWT token" in {
        //TODO: Need to stub a repo here to test
      }
    }
    "write to the history repository" when {
      "a valid message is passed with a valid JWT token" in {
        //TODO: Need to stub a repo here to test
      }
    }
}

object JourneyCacheEndpointsSpec {
  val appConfig: Config = TestSupport.loadConfig
  val jwtSecret: String = appConfig.jwtConfig.secret
  val jwtAlgorithm: String = appConfig.jwtConfig.algorithm

  val validJourneyPostDataJson: String = Source
    .fromResource("web/ValidJourneyPostBody.json")
    .getLines()
    .mkString("")

  val invalidJourneyPostDataJson: String = Source
    .fromResource("web/InvalidJourneyPostBody.json")
    .getLines()
    .mkString("")

  val validJwtToken: String =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyTmFtZSI6IkNocmlzIiwiaWQiOiIxMWZmNWVlNS02NWM3LTRjY2QtODI2ZC1hZDllNTcyMzhhZGIifQ.qT15VVzzodKVZqcYsI-x2dzkWDnb_KiyRFhRYyY3PPE"

  val invalidJwtToken: String =
    "ayJhbGciOiJIUzH1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyTmFtZSI6IkNocmlzIiwiaWQiOiIxMWZmNWVlNS02NWM3LTRjY2QtODI2ZC1hZDllNTcyMzhhZGIifQ.qT15VVzzodKVZqcYsI-x2dzkWDnb_KiyRFhRYyY3PPE"

  val validPostcodesResult: Journey = Journey(
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

  val getJourneyEndpoint: String = "/journey"
}
