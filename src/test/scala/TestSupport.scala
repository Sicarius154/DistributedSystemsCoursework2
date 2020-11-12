package test

import domain.HardcodedJourneyCache

object TestSupport {
  def withHardcodedJourneyCache()(f: HardcodedJourneyCache => Unit): Unit = {
    HardcodedJourneyCache().map(cache => f(cache))
  }
}
