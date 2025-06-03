package ru.filippov.otus.roombooking.config
import pureconfig.ConfigSource
import pureconfig.generic.auto._

case class AppConfig(port: Int, host: String)

/**
 * Метод load используется для загрузки конфигурации приложения.
 * Он использует ConfigSource для загрузки конфигурации из источника по умолчанию.
 *
 * @return AppConfig - объект конфигурации приложения.
 */
object AppConfig {
  def load(): AppConfig = {
    ConfigSource.default.at("app.server").loadOrThrow[AppConfig]
  }
}