package ru.filippov.otus.roombooking.config

import io.getquill.{PostgresJdbcContext, SnakeCase}
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import com.typesafe.config.ConfigFactory
import cats.effect.{IO, Resource}

/**
 * Конфигурация подключения к базе данных PostgreSQL.
 * @param url URL подключения к базе данных
 * @param user Имя пользователя
 * @param password Пароль пользователя
 */
case class DatabaseConfig(
  url: String,
  user: String,
  password: String
)

/**
 * Объект для работы с конфигурацией базы данных.
 */
object DatabaseConfig {
  /**
   * Загружает конфигурацию из файла application.conf.
   * @return Конфигурация базы данных
   */
  def load(): DatabaseConfig = {
    ConfigSource.default.at("app.database").loadOrThrow[DatabaseConfig]
  }

  /**
   * Создает контекст базы данных.
   * @param config Конфигурация базы данных
   * @return Контекст базы данных
   */
  def createContext(config: DatabaseConfig): Resource[IO, PostgresJdbcContext[SnakeCase.type]] = {
    Resource.make(
      IO {
        val dbConfig = ConfigFactory.parseString(s"""
          |ctx {
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
        new PostgresJdbcContext(SnakeCase, dbConfig.getConfig("ctx"))
      }
    )(ctx => IO(ctx.close()))
  }
} 