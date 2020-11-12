package web

import domain.searches.Search
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}
import domain.journeys.{Line, Route, Journey}
import io.circe.{Decoder, Encoder}

package object web {
  implicit val searchDecoder: Decoder[Search] =
    deriveDecoder
  implicit val searchEncoder: Encoder[Search] =
    deriveEncoder
  implicit val journeyDecoder: Decoder[Journey] =
    deriveDecoder
  implicit val journeyEncoder: Encoder[Journey] =
    deriveEncoder
  implicit val historyDecoder: Decoder[UserHistory] =
    deriveDecoder
  implicit val historyEncoder: Encoder[UserHistory] =
    deriveEncoder
  implicit val routeDecoder: Decoder[Route] =
    deriveDecoder
  implicit val routeEncoder: Encoder[Route] =
    deriveEncoder
  implicit val lineDecoder: Decoder[Line] =
    deriveDecoder
  implicit val lineEncoder: Encoder[Line] =
    deriveEncoder
  implicit val insertJourneyRequestDecoder: Decoder[InsertJourneyRequest] =
    deriveDecoder
  implicit val insertJourneyRequestEncoder: Encoder[InsertJourneyRequest] =
    deriveEncoder
}
