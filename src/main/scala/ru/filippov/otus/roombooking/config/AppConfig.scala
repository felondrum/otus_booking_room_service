package ru.filippov.otus.roombooking.config
import pureconfig.ConfigSource
import pureconfig.generic.auto._

case class AppConfig(port: Int, host: String)

object AppConfig {
  def load(): AppConfig = {
    ConfigSource.default.at("app.server").loadOrThrow[AppConfig]
  }
}