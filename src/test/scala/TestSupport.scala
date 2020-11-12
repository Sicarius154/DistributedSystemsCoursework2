package test

import java.nio.charset.StandardCharsets

import config.Config
import domain.journeys.HardcodedJourneyCache
import io.finch.Input
import pureconfig.ConfigSource
import pureconfig.generic.auto._ //required

object TestSupport {
  def withHardcodedJourneyCache()(f: HardcodedJourneyCache => Unit): Unit = {
    HardcodedJourneyCache().map(cache => f(cache))
  }

  def loadConfig: Config =
    ConfigSource.default.loadOrThrow[Config]


  def setReqJsonBodyAndSize(req: Input, body: String): Unit = {
    //TODO: Find a more pure and cleaner way to create these Requests
    req.request.contentLength = body
      .getBytes(StandardCharsets.UTF_8)
      .length

    req.request
      .setContentString(body)

    req.request.setContentTypeJson()
  }
}
