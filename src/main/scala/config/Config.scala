package config

case class Config(journeyCacheServiceConfig: JourneyCacheServiceConfig)

case class JourneyCacheServiceConfig(port: Int)
