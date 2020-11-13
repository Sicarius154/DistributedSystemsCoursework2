package domain.journeys
import java.util.concurrent.Executors

import cats.implicits._
import cats.data.EitherT
import domain.{JourneyID, Postcode}
import doobie.{Query0, Transactor}
import cats.effect.{ContextShift, IO, Blocker}
import cats._
import cats.data._
import cats.implicits._
import doobie._
import doobie.implicits._
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import config.DatabaseConfig
import doobie.hikari.HikariTransactor
import doobie.implicits.toSqlInterpolator
import io.circe.Json
import org.postgresql.util.PSQLException
import org.slf4j.{LoggerFactory, Logger}
import scala.concurrent.ExecutionContext

//TODO: Use prepared statements
object PersistedJourneyCacheQueries {
  def getJourneyByIdQuery(id: String): Query0[JourneyDbResult] =
    sql"""SELECT journeyID, start, end, blob FROM journeys.journey WHERE journeyID = $id"""
      .query[JourneyDbResult]

  def getJourneyByPostcodesQuery(
      start: Postcode,
      end: Postcode
  ): Query0[JourneyDbResult] =
    sql"""SELECT journeyID, start, end, blob FROM journeys.journey WHERE start = $start AND end = $end"""
      .query[JourneyDbResult]
}

class PersistentJourneyCache(transactor: Transactor[IO]) extends JourneyCache {
  private def resultToJourney(journeyResult: JourneyDbResult): Option[Journey] = {
    val jsonBlob: Option[JourneyDbResultBlob] =
      Json.fromString(journeyResult.blob).as[JourneyDbResultBlob].toOption

    for {
      blob <- jsonBlob
    } yield Journey(
      journeyResult.journeyID,
      journeyResult.start,
      journeyResult.end,
      blob.routes,
      blob.meanJourneyTime,
      blob.includesNoChangeRoute
    )
  }

  override def getJourneyByPostcodes(
      start: Postcode,
      end: Postcode
  ): OptionT[IO, Journey] = {
    val dbResult = for {
      dbResult <-
        PersistedJourneyCacheQueries
          .getJourneyByPostcodesQuery(start, end)
          .to[List]
          .transact(transactor)
    } yield resultToJourney(dbResult.head) //TODO: Handle when no head

    OptionT(dbResult)
  }

  override def getJourneyByJourneyID(journeyID: JourneyID): OptionT[IO, Journey] = {
    val dbResult = for {
      dbResult <-
        PersistedJourneyCacheQueries
          .getJourneyByIdQuery(journeyID)
          .to[List]
          .transact(transactor)
      journeyResult = resultToJourney(dbResult.head) //TODO: Handle when no head
    } yield journeyResult

    OptionT(dbResult)
  }
  override def insertJourney(journey: Journey): IO[Unit] = ???
}

object PersistentJourneyCache {
  val logger: Logger = LoggerFactory.getLogger("PersistedJourneyCache")

  def apply(
      databaseConfig: DatabaseConfig
  )(implicit
      ec: ExecutionContext,
      cs: ContextShift[IO]
  ): EitherT[IO, String, PersistentJourneyCache] =
    for {
      transactor <- hikariTransactor(databaseConfig)
    } yield new PersistentJourneyCache(transactor)

  private def hikariTransactor(
      databaseConfig: DatabaseConfig
  )(implicit
      ec: ExecutionContext,
      cs: ContextShift[IO]
  ): EitherT[IO, String, HikariTransactor[IO]] =
    EitherT(
      IO {
        val databaseExecutionContext = ExecutionContext.fromExecutor(
          Executors.newFixedThreadPool(databaseConfig.connection.poolSize)
        )

        val hikariConfig = new HikariConfig()
        hikariConfig.setJdbcUrl(databaseConfig.connection.connectionString)
        hikariConfig.setUsername(databaseConfig.login.username)
        hikariConfig.setPassword(databaseConfig.login.password)
        hikariConfig.setMaximumPoolSize(databaseConfig.connection.poolSize)

        Right(
          HikariTransactor.apply[IO](
            new HikariDataSource(hikariConfig),
            ec,
            Blocker.liftExecutionContext(
              databaseExecutionContext
            )
          )
        )
      }.redeem(
        { ex =>
          logger.error(
            s"Error obtaining transactor at URL ${databaseConfig.connection.connectionString}."
          )
          ex match {
            case _: PSQLException =>
              logger.error(
                s"Check credentials and connection String. Is the database alive?"
              )
            case _ =>
              logger.error(s"Unknown exception...")
          }
          Left(ex.getMessage)
        },
        { transactor =>
          logger.info("Acquired database transactor")
          transactor
        }
      )
    )
}
