package domain.journeys

import domain.{Postcode, JourneyID}


case class JourneyDbResult(
                          journeyID: JourneyID,
                          start: Postcode,
                          end: Postcode,
                          blob: String
                        )