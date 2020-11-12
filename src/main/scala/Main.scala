import cats.effect.{IO, ExitCode, IOApp}

import scala.concurrent.ExecutionContext

object Main extends IOApp {
  implicit val ec: ExecutionContext = ExecutionContext.global
  override def run(args: List[String]): IO[ExitCode] = ???
}
