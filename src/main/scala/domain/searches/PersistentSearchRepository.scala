package domain.searches

import java.util.concurrent.Executors

import io.circe.generic.auto._
import io.circe.parser._
import cats.implicits._
import io.circe.syntax._
import cats.data.EitherT
import domain.{JourneyID, Postcode, UserID}
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
 *Queries for the search repository
 */
object PersistedSearchRepositoryQueries {
  def insertNewSearch(
      userID: UserID,
      journeyID: JourneyID
  ): doobie.ConnectionIO[Int] =
    sql"""INSERT INTO journeys.search("journeyid", "userid") VALUES($journeyID, $userID)""".update.run

  def getUserSearchesByUserID(userID: UserID): doobie.Query0[Search] =
    sql"""SELECT "journeyid", "userid" FROM journeys.search WHERE "userid" = $userID"""
      .query[Search]
}

class PersistentSearchRepository(transactor: Transactor[IO])(implicit
    logger: Logger
) extends SearchRepository {
  override def getUserSearches(userID: UserID): Nested[IO, List, Search] =
    Nested(
      PersistedSearchRepositoryQueries
        .getUserSearchesByUserID(userID)
        .to[List]
        .transact(transactor)
    )

  override def addUserSearch(userID: UserID, journeyID: JourneyID): IO[Unit] =
    PersistedSearchRepositoryQueries
      .insertNewSearch(userID, journeyID)
      .transact(transactor)
      .map(_ => ()) //map to unit as we don't care about values
}

object PersistentSearchRepository {
  private implicit val logger: Logger =
    LoggerFactory.getLogger("PersistedSearchRepository")

  def apply(
      databaseConfig: DatabaseConfig
  )(implicit
      ec: ExecutionContext,
      cs: ContextShift[IO]
  ): EitherT[IO, String, PersistentSearchRepository] =
    hikariTransactor(databaseConfig)
      .map(transactor => new PersistentSearchRepository(transactor))

  /**
   * Execution context to push DB operations on to
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
   * Acquire a Hikari transactor for DB operations
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

        //Return the acquired transactor as a Right
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
