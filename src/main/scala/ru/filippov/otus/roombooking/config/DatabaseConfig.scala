package ru.filippov.otus.roombooking.config

import io.getquill.{PostgresJdbcContext, SnakeCase}
import cats.effect.{IO, Resource}
import com.typesafe.config.ConfigFactory

case class DatabaseConfig(
  url: String,
  user: String,
  password: String,
  driver: String = "org.postgresql.Driver"
)

object DatabaseConfig {
  def createContext(config: DatabaseConfig): Resource[IO, PostgresJdbcContext[SnakeCase.type]] = {
    Resource.make(
      IO {
        val dbConfig = ConfigFactory.parseString(s"""
          |app.database {
          |  dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
          |  dataSource {
          |    url = "${config.url}"
          |    user = "${config.user}"
          |    password = "${config.password}"
          |  }
          |  connectionTimeout = 30000
          |  maximumPoolSize = 10
          |}
          |""".stripMargin)
        new PostgresJdbcContext(SnakeCase, dbConfig.getConfig("app.database"))
      }
    )(ctx => IO(ctx.close()))
  }
} 