package application

import domain.journeys
import cats.Applicative.ops.toAllApplicativeOps
import cats.Parallel
import cats.effect.{ContextShift, Blocker, Resource, ExitCode, IO}
import com.twitter.finagle.{ListeningServer, Service, Http}
import com.twitter.finagle.http.{Request, Response}
import config.Config
import io.finch.{Bootstrap, Application, ToAsync}
import org.slf4j.{LoggerFactory, Logger}
import pureconfig.ConfigSource
import com.twitter.util.Future
import domain.journeys.{JourneyCache, HardcodedJourneyCache, PersistentJourneyCache}
import domain.searches.{PersistentSearchRepository, HardcodedSearchRepository, SearchRepository}
import web.Endpoints
import io.finch.circe._
import io.circe.generic.auto._
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContext

class Application()(implicit
    ec: ExecutionContext,
    cs: ContextShift[IO],
    parallel: Parallel[IO]
) {
  private val logger: Logger = LoggerFactory.getLogger("Application")

  def execute(): IO[ExitCode] = {
    logger.info("Starting application")

    val conf = loadConfig

    PersistentJourneyCache(conf.databaseConfig).flatMap {
      journeyCache =>
        PersistentSearchRepository(conf.databaseConfig).map {
          searchRepository =>
            val server =
              Resource.make(
                serve(
                  journeyCache,
                  searchRepository,
                  conf.journeyCacheServiceConfig.port,
                  conf.jwtConfig.secret,
                  conf.jwtConfig.algorithm
                )
              )(s =>
                IO.suspend(implicitly[ToAsync[Future, IO]].apply(s.close()))
              )
            server.use(_ => IO.never).as(ExitCode.Success)
        }
    }.value
      .flatMap(serverRes =>
        (serverRes match {
          case Right(code) => code
          case Left(err: String) => {
            logger.error(s"Exiting with error: $err")
            IO.pure(ExitCode.Error)
          }
        })
          .map(res => res)
      )
  }

  private def serve(
      journeyCache: JourneyCache,
      searchRepository: SearchRepository,
      portNumber: Int,
      jwtSecret: String,
      jwtAlgorithm: String
  ): IO[ListeningServer] =
    IO(
      Http.server
        .serve(
          s":$portNumber",
          service(journeyCache, searchRepository, jwtSecret, jwtAlgorithm)
        )
    )

  private def service(
      journeyCache: JourneyCache,
      searchRepository: SearchRepository,
      jwtSecret: String,
      jwtAlgorithm: String
  ): Service[Request, Response] =
    Bootstrap
      .serve[Application.Json](
        Endpoints.all(
          journeyCache,
          searchRepository,
          jwtSecret,
          jwtAlgorithm
        )
      )
      .toService

  private def loadConfig: Config =
    ConfigSource.default.loadOrThrow[Config]
}
