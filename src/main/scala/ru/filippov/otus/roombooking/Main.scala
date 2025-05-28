package ru.filippov.otus.roombooking

import cats.effect.{IO, IOApp}
import ru.filippov.otus.roombooking.config.DatabaseConfig
import ru.filippov.otus.roombooking.http.Controllers
import ru.filippov.otus.roombooking.repository.{UserRepository, RoomRepository, BookingRepository}
import ru.filippov.otus.roombooking.service.{UserService, RoomService, BookingService}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import com.comcast.ip4s.{Host, Port}
import pureconfig._
import pureconfig.generic.auto._
import cats.syntax.semigroupk._
import org.http4s.server.middleware.Logger
import pureconfig.ConfigSource
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor

object Main extends IOApp.Simple {
  def run: IO[Unit] = {
    for {
      config <- IO(ConfigSource.default.at("app.database").loadOrThrow[DatabaseConfig])
      _ <- runMigrations(config)
      ctx <- DatabaseConfig.createContext(config).use { ctx =>
        val userRepository = new UserRepository(ctx)
        val roomRepository = new RoomRepository(ctx)
        val bookingRepository = new BookingRepository(ctx)

        val userService = new UserService(userRepository)
        val roomService = new RoomService(roomRepository)
        val bookingService = new BookingService(bookingRepository, roomRepository)

        val httpApp = Router(
          "/api" -> (
            Controllers.userRoutes(userService) <+>
            Controllers.roomRoutes(roomService) <+>
            Controllers.bookingRoutes(bookingService)
          )
        ).orNotFound

        val httpAppWithLogging = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

        EmberServerBuilder
          .default[IO]
          .withHost(Host.fromString("0.0.0.0").get)
          .withPort(Port.fromInt(8080).get)
          .withHttpApp(httpAppWithLogging)
          .build
          .use(_ => IO.never)
      }
    } yield ctx
  }

  private def runMigrations(config: DatabaseConfig): IO[Unit] = IO {
    val connection = java.sql.DriverManager.getConnection(
      config.url,
      config.user,
      config.password
    )
    
    try {
      val database = DatabaseFactory.getInstance()
        .findCorrectDatabaseImplementation(new JdbcConnection(connection))
      
      val liquibase = new Liquibase(
        "db/changelog/db.changelog-master.xml",
        new ClassLoaderResourceAccessor(),
        database
      )
      
      liquibase.update("")
    } finally {
      connection.close()
    }
  }
}