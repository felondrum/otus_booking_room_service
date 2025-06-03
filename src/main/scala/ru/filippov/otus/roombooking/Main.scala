package ru.filippov.otus.roombooking

import cats.effect.{IO, IOApp}
import cats.effect.unsafe.implicits.global
import ru.filippov.otus.roombooking.config.DatabaseConfig
import ru.filippov.otus.roombooking.repository.{UserRepository, RoomRepository, BookingRepository}
import ru.filippov.otus.roombooking.service.{UserService, RoomService, BookingService}
import ru.filippov.otus.roombooking.http.Controllers
import ru.filippov.otus.roombooking.http.OpenApiEndpoints
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s.{Host, Port}
import org.http4s.server.Router
import org.http4s.server.middleware.{CORS, Logger}
import org.http4s.HttpApp
import sttp.tapir.server.http4s.Http4sServerInterpreter
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor

/**
 * Главный класс приложения для бронирования комнат.
 * Реализует REST API сервис с использованием http4s и PostgreSQL.
 */
object Main extends IOApp.Simple {
  /**
   * Основной метод, запускающий приложение.
   * @return IO[Unit] - результат выполнения приложения
   */
  override def run: IO[Unit] = {
    // Загружаем конфигурацию
    val dbConfig = DatabaseConfig.load()

    // Создаем контекст базы данных
    DatabaseConfig.createContext(dbConfig).use { ctx =>
      // Запускаем миграции
      runMigrations(dbConfig).unsafeRunSync()

      // Инициализируем репозитории
      val userRepository = new UserRepository(ctx)
      val roomRepository = new RoomRepository(ctx)
      val bookingRepository = new BookingRepository(ctx)

      // Инициализируем сервисы
      val userService = new UserService(userRepository)
      val roomService = new RoomService(roomRepository)
      val bookingService = new BookingService(bookingRepository, roomRepository)

      // Создаем HTTP роуты
      val apiRoutes = Controllers.createRoutes(userService, roomService, bookingService)
      val swaggerRoutes = Http4sServerInterpreter[IO]().toRoutes(OpenApiEndpoints.swaggerEndpoints)
      val routes = Router(
        "/api" -> apiRoutes,
        "/" -> swaggerRoutes
      )

      // Создаем HTTP приложение с CORS и логированием
      val httpApp: HttpApp[IO] = CORS(Logger.httpApp(true, true)(routes.orNotFound))

      // Запускаем сервер
      EmberServerBuilder
        .default[IO]
        .withHost(Host.fromString("0.0.0.0").get)
        .withPort(Port.fromInt(8080).get)
        .withHttpApp(httpApp)
        .build
        .use(_ => IO.never)
    }
  }

  /**
   * Запускает миграции базы данных с помощью Liquibase.
   * @param config Конфигурация базы данных
   * @return IO[Unit] - результат выполнения миграций
   */
  private def runMigrations(config: DatabaseConfig): IO[Unit] = IO {
    println(s"Running migrations with config: url=${config.url}, user=${config.user}")
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
      
      println("Starting database migration...")
      liquibase.update("")
      println("Database migration completed successfully")
    } catch {
      case e: Exception =>
        println(s"Migration failed: ${e.getMessage}")
        throw e
    } finally {
      connection.close()
    }
  }
}