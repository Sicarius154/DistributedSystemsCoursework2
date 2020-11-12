package application

import domain.journeys
import cats.Applicative.ops.toAllApplicativeOps
import cats.effect.{ContextShift, Blocker, Resource, ExitCode, IO}
import com.twitter.finagle.{ListeningServer, Service, Http}
import com.twitter.finagle.http.{Request, Response}
import config.Config
import io.finch.{Bootstrap, Application, ToAsync}
import org.slf4j.{LoggerFactory, Logger}
import pureconfig.ConfigSource
import com.twitter.util.Future
import domain.journeys.{JourneyCache, HardcodedJourneyCache}
import web.Endpoints
import io.finch.circe._
import io.circe.generic.auto._
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContext

class Application()(implicit ec: ExecutionContext, cs: ContextShift[IO]) {
  private val logger: Logger = LoggerFactory.getLogger("Application")

  def execute(): IO[ExitCode] = {
    logger.info("Starting application")

    val conf = loadConfig

    for {
      serverRes <- HardcodedJourneyCache().map { journeyCache =>
        val server =
          Resource.make(
            serve(journeyCache, conf.journeyCacheServiceConfig.port)
          )(s => IO.suspend(implicitly[ToAsync[Future, IO]].apply(s.close())))
        server.use(_ => IO.never).as(ExitCode.Success)
      }.value

      res <- serverRes match {
        case Right(code)       => code
        case Left(err: String) => {
          logger.error(s"Exiting with error: $err")
          IO.pure(ExitCode.Error)
        }
      }
    } yield res
  }

  private def serve(
      cache: JourneyCache,
      portNumber: Int
  ): IO[ListeningServer] =
    IO(Http.server.serve(s":$portNumber", service(cache)))

  private def service(cache: JourneyCache): Service[Request, Response] =
    Bootstrap
      .serve[Application.Json](Endpoints.journeyCacheEndpoints(cache))
      .toService

  private def loadConfig: Config =
    ConfigSource.default.loadOrThrow[Config]
}
