package domain

import domain.searches.Search
import web.{UserHistory, InsertJourneyRequest}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

package object journeys {
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
  implicit val blobDecoder: Decoder[JourneyDbResultBlob] =
    deriveDecoder
  implicit val blobEncoder: Encoder[JourneyDbResultBlob] =
    deriveEncoder
}
