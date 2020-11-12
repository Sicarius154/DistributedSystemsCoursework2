package domain.searches

import cats.effect.IO
import domain.{UserID, JourneyID}

trait SearchRepository {
  def getUserSearches(userID: UserID): IO[List[Search]]
  def addUserSearch(userID: UserID, journeyID: JourneyID): IO[Unit]
}
