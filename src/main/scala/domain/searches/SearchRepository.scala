package domain.searches

import cats.data.Nested
import cats.effect.IO
import domain.{JourneyID, UserID}

trait SearchRepository {
  def getUserSearches(userID: UserID): Nested[IO, List, Search]
  def addUserSearch(userID: UserID, journeyID: JourneyID): IO[Unit]
}
