package domain.journeys
import java.util.concurrent.Executors

import io.circe.generic.auto._
import io.circe.parser._
import cats.implicits._
import io.circe.syntax._
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

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

//TODO: Use prepared statements
/**
 * Queries for the Journey table
 */
object PersistedJourneyCacheQueries {
  def getJourneyByIdQuery(id: String): Query0[JourneyDbResult] =
    sql"""SELECT "journeyid", "start", "end", "blob" FROM journeys.journey WHERE journeyid = $id"""
      .query[JourneyDbResult]

  def getJourneyByPostcodesQuery(
      start: Postcode,
      end: Postcode
  ): Query0[JourneyDbResult] =
    sql"""SELECT "journeyid", "start", "end", "blob" FROM journeys.journey WHERE start = $start AND "end" = $end"""
      .query[JourneyDbResult]

  def insertNewJourney(
      journeyID: JourneyID,
      start: Postcode,
      end: Postcode,
      blob: String
  ): doobie.ConnectionIO[Int] =
    sql"""INSERT INTO journeys.journey("journeyid", "start", "end", "blob") VALUES($journeyID, $start, $end, $blob)""".update.run
}

class PersistentJourneyCache(transactor: Transactor[IO]) extends JourneyCache {
  private val logger: Logger = LoggerFactory.getLogger("PersistentJourneyCache")

  /**
   * Takes a database transaction result, validates the JSON blob and converts it to a Journey
   * @param journeyResult
   * @return
   */
  private def resultToJourney(journeyResult: JourneyDbResult): Option[Journey] =
    for {
      blob <- decode[JourneyDbResultBlob](journeyResult.blob).toOption
      _ = logger.info(s"Decoding DB result from journey table")
      routes <- NonEmptyList.fromList(blob.routes)
    } yield Journey(
      journeyResult.journeyID,
      journeyResult.start,
      journeyResult.end,
      routes,
      blob.meanJourneyTime,
      blob.includesNoChangeRoute
    )

  override def getJourneyByPostcodes(
      start: Postcode,
      end: Postcode
  ): OptionT[IO, Journey] =
    OptionT(
      PersistedJourneyCacheQueries
        .getJourneyByPostcodesQuery(start, end)
        .to[List]
        .transact(transactor)
        .map(dbResult => resultToJourney(dbResult.head))
    ) //TODO: Handle when no head

  override def getJourneyByJourneyID(
      journeyID: JourneyID
  ): OptionT[IO, Journey] =
    OptionT(
      PersistedJourneyCacheQueries
        .getJourneyByIdQuery(journeyID)
        .to[List]
        .transact(transactor)
        .map(dbResult =>
          resultToJourney(dbResult.head)
        ) //TODO: Handle when no head
    )
  override def insertJourney(journey: Journey): IO[Unit] = {
    logger.info(s"Inserting into journeys table ID ${journey.journeyID}")

    val blob = JourneyDbResultBlob(
      journey.routes.toList,
      journey.meanJourneyTime,
      journey.includesNoChangeRoute
    )

    PersistedJourneyCacheQueries
      .insertNewJourney(
        journey.journeyID,
        journey.start,
        journey.end,
        blob.asJson.toString
      )
      .transact(transactor)
      .map(_ => ()) //We don't care about values, so map to Unit (())
  }
}

object PersistentJourneyCache {
  val logger: Logger = LoggerFactory.getLogger("PersistedJourneyCache")

  def apply(
      databaseConfig: DatabaseConfig
  )(implicit
      ec: ExecutionContext,
      cs: ContextShift[IO]
  ): EitherT[IO, String, PersistentJourneyCache] =
    hikariTransactor(databaseConfig)
      .map(transactor => new PersistentJourneyCache(transactor))

  /**
   * Execution context for db operations. Size determined by typesafe config
   * @param poolSize
   * @return
   */
  private def databaseExecutionContext(
      poolSize: Int
  ): ExecutionContextExecutor =
    ExecutionContext.fromExecutor(
      Executors.newFixedThreadPool(poolSize)
    )

  /**
   * Acquire Hikari transactor for database transactions
   * @param databaseConfig
   * @param ec
   * @param cs
   * @return
   */

  private def hikariTransactor(
      databaseConfig: DatabaseConfig
  )(implicit
      ec: ExecutionContext,
      cs: ContextShift[IO]
  ): EitherT[IO, String, HikariTransactor[IO]] =
    EitherT(
      IO {
        val dbEc = databaseExecutionContext(databaseConfig.connection.poolSize)

        val hikariConfig = new HikariConfig()
        hikariConfig.setJdbcUrl(databaseConfig.connection.connectionString)
        hikariConfig.setUsername(databaseConfig.login.username)
        hikariConfig.setPassword(databaseConfig.login.password)
        hikariConfig.setMaximumPoolSize(databaseConfig.connection.poolSize)

        //Return the transactor as Right
        Right(
          HikariTransactor.apply[IO](
            new HikariDataSource(hikariConfig),
            ec,
            Blocker.liftExecutionContext(
              dbEc
            )
          )
        )
      }.redeem(
        (ex: Throwable) =>
          handleHikariTransactorAcquisitionError(
            ex,
            databaseConfig.connection.connectionString
          ),
        (acq: Right[String, HikariTransactor[IO]]) =>
          handleHikariTransactorAcquisition(acq)
      )
    )

  private def handleHikariTransactorAcquisitionError(
      ex: Throwable,
      databaseConnectionString: String
  ): Either[String, HikariTransactor[IO]] = {
    logger.error(
      s"Error obtaining transactor at URL $databaseConnectionString."
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
  }

  private def handleHikariTransactorAcquisition(
      transactor: Either[String, HikariTransactor[IO]]
  ): Either[String, HikariTransactor[IO]] = {
    logger.info("Acquired database transactor")
    transactor
  }
}
