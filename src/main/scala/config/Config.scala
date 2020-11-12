package config

case class Config(journeyCacheServiceConfig: JourneyCacheServiceConfig, jwtConfig: JwtConfig)

case class JourneyCacheServiceConfig(port: Int)

case class JwtConfig(secret: String, algorithm: String)