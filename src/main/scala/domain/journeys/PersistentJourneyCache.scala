//package domain.journeys
//import java.util.concurrent.Executors
//
//import cats.data.EitherT
//import domain.{JourneyID, Postcode}
//import doobie.{Query0, Transactor}
//import cats.effect.{ContextShift, IO, Blocker}
//import cats._
//import cats.data._
//import cats.implicits._
//import doobie._
//import doobie.implicits._
//import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
//import config.DatabaseConfig
//import doobie.hikari.HikariTransactor
//import doobie.implicits.toSqlInterpolator
//import org.postgresql.util.PSQLException
//import org.slf4j.{LoggerFactory, Logger}
//import scala.concurrent.ExecutionContext
//
////TODO: Use prepared statements
//object PersistedJourneyCacheQueries {
//  def getJourneyByIdQuery(id: String): Query0[Journey] =
//    sql"""SELECT journeyID, start, end, routes, meanJourneyTime, includesNoChangeRoute FROM journeys.journey WHERE journeyID = $id"""
//      .query[Journey]
//
//  def getJourneyByPostcodesQuery(
//      start: Postcode,
//      end: Postcode
//  ): Query0[Journey] =
//    sql"""SELECT journeyID, start, end, routes, meanJourneyTime, includesNoChangeRoute FROM journeys.journey WHERE start = $start AND end = $end"""
//      .query[Journey]
//}
//
//class PersistentJourneyCache(transactor: Transactor[IO]) extends JourneyCache {
//  override def getJourneyByPostcodes(
//      start: Postcode,
//      end: Postcode
//  ): OptionT[IO, Journey] =
//    for {
//      result <-
//        PersistedJourneyCacheQueries
//          .getJourneyByPostcodesQuery(start, end)
//          .to[List]
//          .transact(transactor)
//    } yield OptionT.fromOption(result.headOption)//TODO: We're taking the .head here, as in theory there should only be one entry; but we could probably have a sanity check here
//
//  override def getJourneyByJourneyID(journeyID: JourneyID): IO[Journey] = {
//    val res: OptionT[IO, Journey] = for {
//      result <-
//        PersistedJourneyCacheQueries
//          .getJourneyByIdQuery(journeyID.toString)
//          .to[List]
//          .transact(transactor)
//    } yield OptionT.fromOption(result.headOption)
//  }
//  override def insertJourney(journey: Journey): IO[Unit] = ???
//}
//
//object PersistentJourneyCache {
//  val logger: Logger = LoggerFactory.getLogger("PersistedJourneyCache")
//
//  def apply(
//      databaseConfig: DatabaseConfig
//  )(implicit
//      ec: ExecutionContext,
//      cs: ContextShift[IO]
//  ): EitherT[IO, String, PersistentJourneyCache] =
//    for {
//      transactor <- hikariTransactor(databaseConfig)
//    } yield new PersistentJourneyCache(transactor)
//
//  private def hikariTransactor(
//      databaseConfig: DatabaseConfig
//  )(implicit
//      ec: ExecutionContext,
//      cs: ContextShift[IO]
//  ): EitherT[IO, String, HikariTransactor[IO]] =
//    EitherT(
//      IO {
//        val databaseExecutionContext = ExecutionContext.fromExecutor(
//          Executors.newFixedThreadPool(databaseConfig.connection.poolSize)
//        )
//
//        val hikariConfig = new HikariConfig()
//        hikariConfig.setJdbcUrl(databaseConfig.connection.connectionString)
//        hikariConfig.setUsername(databaseConfig.login.username)
//        hikariConfig.setPassword(databaseConfig.login.password)
//        hikariConfig.setMaximumPoolSize(databaseConfig.connection.poolSize)
//
//        Right(
//          HikariTransactor.apply[IO](
//            new HikariDataSource(hikariConfig),
//            ec,
//            Blocker.liftExecutionContext(
//              databaseExecutionContext
//            )
//          )
//        )
//      }.redeem(
//        { ex =>
//          logger.error(
//            s"Error obtaining transactor at URL ${databaseConfig.connection.connectionString}."
//          )
//          ex match {
//            case _: PSQLException =>
//              logger.error(
//                s"Check credentials and connection String. Is the database alive?"
//              )
//            case _ =>
//              logger.error(s"Unknown exception...")
//          }
//          Left(ex.getMessage)
//        },
//        { transactor =>
//          logger.info("Acquired database transactor")
//          transactor
//        }
//      )
//    )
//}
