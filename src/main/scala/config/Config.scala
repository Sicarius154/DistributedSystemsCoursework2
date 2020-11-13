package config

case class Config(journeyCacheServiceConfig: JourneyCacheServiceConfig, jwtConfig: JwtConfig, databaseConfig: DatabaseConfig)

case class JourneyCacheServiceConfig(port: Int)

case class JwtConfig(secret: String, algorithm: String)

case class DatabaseLogin(username: String, password: String)

case class DatabaseConnection(connectionString: String, poolSize: Int)

case class DatabaseConfig(login: DatabaseLogin, connection: DatabaseConnection)
