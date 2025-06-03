package ru.filippov.otus.roombooking.http

import cats.effect.IO
import ru.filippov.otus.roombooking.model.{User, Room, Booking}
import ru.filippov.otus.roombooking.service.{UserService, RoomService, BookingService, ValidationError}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.HttpRoutes
import org.http4s.circe.jsonOf
import org.http4s.circe.jsonEncoderOf
import java.time.LocalDateTime
import java.util.UUID
import org.http4s.EntityDecoder
import org.http4s.EntityEncoder
import cats.syntax.semigroupk._

/**
 * Объект, содержащий HTTP-контроллеры для работы с пользователями, комнатами и бронированиями.
 * Реализует REST API для управления ресурсами системы бронирования комнат.
 */
object Controllers {
  // Decoders
  implicit val createUserDecoder: EntityDecoder[IO, CreateUserRequest] = jsonOf[IO, CreateUserRequest]
  implicit val createRoomDecoder: EntityDecoder[IO, CreateRoomRequest] = jsonOf[IO, CreateRoomRequest]
  implicit val createBookingDecoder: EntityDecoder[IO, CreateBookingRequest] = jsonOf[IO, CreateBookingRequest]
  implicit val checkAvailabilityDecoder: EntityDecoder[IO, CheckAvailabilityRequest] = jsonOf[IO, CheckAvailabilityRequest]
  implicit val availableRoomsDecoder: EntityDecoder[IO, AvailableRoomsRequest] = jsonOf[IO, AvailableRoomsRequest]
  implicit val availableRoomsByDateDecoder: EntityDecoder[IO, AvailableRoomsByDateRequest] = jsonOf[IO, AvailableRoomsByDateRequest]

  // Encoders
  implicit val userResponseEncoder: EntityEncoder[IO, UserResponse] = jsonEncoderOf[IO, UserResponse]
  implicit val roomResponseEncoder: EntityEncoder[IO, RoomResponse] = jsonEncoderOf[IO, RoomResponse]
  implicit val bookingResponseEncoder: EntityEncoder[IO, BookingResponse] = jsonEncoderOf[IO, BookingResponse]
  implicit val errorResponseEncoder: EntityEncoder[IO, ErrorResponse] = jsonEncoderOf[IO, ErrorResponse]

  // Converters
  /**
   * Преобразует модель пользователя в ответ API
   * @param user Модель пользователя
   * @return Ответ API с информацией о пользователе
   */
  private def toUserResponse(user: User): UserResponse = 
    UserResponse(user.id.toString, user.name, user.email)

  /**
   * Преобразует модель комнаты в ответ API
   * @param room Модель комнаты
   * @return Ответ API с информацией о комнате
   */
  private def toRoomResponse(room: Room): RoomResponse = 
    RoomResponse(room.id.toString, room.name, room.capacity, room.description)

  /**
   * Преобразует модель бронирования в ответ API
   * @param booking Модель бронирования
   * @return Ответ API с информацией о бронировании
   */
  private def toBookingResponse(booking: Booking): BookingResponse = 
    BookingResponse(
      booking.id.toString,
      booking.roomId.toString,
      booking.userId.toString,
      booking.startTime.toString,
      booking.endTime.toString,
      booking.createdAt.toString
    )

  /**
   * Создает маршруты для работы с пользователями
   * @param userService Сервис для работы с пользователями
   * @return HTTP-маршруты для работы с пользователями
   */
  def userRoutes(userService: UserService): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      /**
       * Создает нового пользователя
       * POST /users
       * @return 200 OK с информацией о созданном пользователе или 400 Bad Request в случае ошибки
       */
      case req @ POST -> Root / "users" =>
        for {
          _ <- IO.println("Received POST /users request")
          body <- req.as[CreateUserRequest]
          _ <- IO.println(s"Request body: $body")
          result <- userService.createUser(body.name, body.email).attempt
          response <- result match {
            case Right(Right(user)) => Ok(toUserResponse(user).asJson)
            case Right(Left(error: ValidationError)) => BadRequest(ErrorResponse(error.message).asJson)
            case Left(error) => BadRequest(ErrorResponse(error.getMessage).asJson)
          }
        } yield response

      /**
       * Получает список всех пользователей
       * GET /users
       * @return 200 OK со списком пользователей
       */
      case GET -> Root / "users" =>
        for {
          _ <- IO.println("Received GET /users request")
          users <- userService.getAllUsers
          _ <- IO.println(s"Found users: $users")
          response <- Ok(users.map(toUserResponse).asJson)
        } yield response
    }
  }

  /**
   * Создает роуты для работы с комнатами
   * @param roomService Сервис для работы с комнатами
   * @return HTTP-маршруты для работы с комнатами
   */
  def roomRoutes(roomService: RoomService): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      /**
       * Создает новую комнату
       * POST /rooms
       * @return 200 OK с информацией о созданной комнате или 400 Bad Request в случае ошибки
       */
      case req @ POST -> Root / "rooms" =>
        for {
          _ <- IO.println("Received POST /rooms request")
          body <- req.as[CreateRoomRequest]
          _ <- IO.println(s"Request body: $body")
          result <- roomService.createRoom(body.name, body.capacity, body.description).attempt
          response <- result match {
            case Right(Right(room)) => Ok(toRoomResponse(room).asJson)
            case Right(Left(error: ValidationError)) => BadRequest(ErrorResponse(error.message).asJson)
            case Left(error) => BadRequest(ErrorResponse(error.getMessage).asJson)
          }
        } yield response

      /**
       * Удаляет комнату по ID
       * DELETE /rooms/{id}
       * @return 200 OK при успешном удалении
       */
      case DELETE -> Root / "rooms" / UUIDVar(id) =>
        for {
          _ <- IO.println(s"Received DELETE /rooms/$id request")
          _ <- roomService.deleteRoom(id)
          _ <- IO.println(s"Deleted room: $id")
          response <- Ok()
        } yield response

      /**
       * Получает список всех комнат
       * GET /rooms
       * @return 200 OK со списком комнат
       */
      case GET -> Root / "rooms" =>
        for {
          _ <- IO.println("Received GET /rooms request")
          rooms <- roomService.getAllRooms
          _ <- IO.println(s"Found rooms: $rooms")
          response <- Ok(rooms.map(toRoomResponse).asJson)
        } yield response

      /**
       * Получает список доступных комнат на указанный период
       * POST /rooms/available
       * @return 200 OK со списком доступных комнат или 400 Bad Request в случае ошибки
       */
      case req @ POST -> Root / "rooms" / "available" =>
        for {
          _ <- IO.println("Received POST /rooms/available request")
          body <- req.as[AvailableRoomsRequest]
          _ <- IO.println(s"Request body: $body")
          startTime = LocalDateTime.parse(body.startTime)
          endTime = LocalDateTime.parse(body.endTime)
          result <- roomService.getAvailableRooms(startTime, endTime, body.capacity).attempt
          response <- result match {
            case Right(Right(rooms)) => Ok(rooms.map(toRoomResponse).asJson)
            case Right(Left(error: ValidationError)) => BadRequest(ErrorResponse(error.message).asJson)
            case Left(error) => BadRequest(ErrorResponse(error.getMessage).asJson)
          }
        } yield response

      /**
       * Получает список доступных комнат на указанную дату
       * POST /rooms/available/date
       * @return 200 OK со списком доступных комнат или 400 Bad Request в случае ошибки
       */
      case req @ POST -> Root / "rooms" / "available" / "date" =>
        for {
          _ <- IO.println("Received POST /rooms/available/date request")
          body <- req.as[AvailableRoomsByDateRequest]
          _ <- IO.println(s"Request body: $body")
          date = LocalDateTime.parse(body.date + "T00:00:00")
          result <- roomService.getAvailableRoomsByDate(date, body.capacity).attempt
          response <- result match {
            case Right(Right(rooms)) => Ok(rooms.map(toRoomResponse).asJson)
            case Right(Left(error: ValidationError)) => BadRequest(ErrorResponse(error.message).asJson)
            case Left(error) => BadRequest(ErrorResponse(error.getMessage).asJson)
          }
        } yield response
    }
  }

  /**
   * Создает роуты для работы с бронированиями
   * @param bookingService Сервис для работы с бронированиями
   * @return HTTP-маршруты для работы с бронированиями
   */
  def bookingRoutes(bookingService: BookingService): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      /**
       * Создает новое бронирование
       * POST /bookings
       * @return 200 OK с информацией о созданном бронировании или 400 Bad Request в случае ошибки
       */
      case req @ POST -> Root / "bookings" =>
        for {
          _ <- IO.println("Received POST /bookings request")
          body <- req.as[CreateBookingRequest]
          _ <- IO.println(s"Request body: $body")
          roomId = UUID.fromString(body.roomId)
          userId = UUID.fromString(body.userId)
          startTime = LocalDateTime.parse(body.startTime)
          endTime = LocalDateTime.parse(body.endTime)
          bookingResult <- bookingService.createBooking(roomId, userId, startTime, endTime).attempt
          response <- bookingResult match {
            case Right(Right(booking)) => Ok(toBookingResponse(booking).asJson)
            case Right(Left(error: ValidationError)) => BadRequest(ErrorResponse(error.message).asJson)
            case Left(error) => BadRequest(ErrorResponse(error.getMessage).asJson)
          }
        } yield response

      /**
       * Удаляет бронирование по ID
       * DELETE /bookings/{id}
       * @return 200 OK при успешном удалении
       */
      case DELETE -> Root / "bookings" / UUIDVar(id) =>
        for {
          _ <- IO.println(s"Received DELETE /bookings/$id request")
          _ <- bookingService.deleteBooking(id)
          _ <- IO.println(s"Deleted booking: $id")
          response <- Ok()
        } yield response

      /**
       * Получает список всех бронирований
       * GET /bookings
       * @return 200 OK со списком бронирований
       */
      case GET -> Root / "bookings" =>
        for {
          _ <- IO.println("Received GET /bookings request")
          bookings <- bookingService.getAllBookings
          _ <- IO.println(s"Found bookings: $bookings")
          response <- Ok(bookings.map(toBookingResponse).asJson)
        } yield response

      /**
       * Получает список бронирований по ID пользователя
       * GET /bookings/user/{userId}
       * @return 200 OK со списком бронирований пользователя
       */
      case GET -> Root / "bookings" / "user" / UUIDVar(userId) =>
        for {
          _ <- IO.println(s"Received GET /bookings/user/$userId request")
          bookings <- bookingService.getBookingsByUserId(userId)
          _ <- IO.println(s"Found bookings for user $userId: $bookings")
          response <- Ok(bookings.map(toBookingResponse).asJson)
        } yield response

      /**
       * Проверяет доступность комнаты на указанный период
       * GET /bookings/check
       * @return 200 OK с результатом проверки (true/false) или 400 Bad Request в случае ошибки
       */
      case req @ GET -> Root / "bookings" / "check" =>
        for {
          _ <- IO.println("Received GET /bookings/check request")
          body <- req.as[CheckAvailabilityRequest]
          _ <- IO.println(s"Request body: $body")
          roomId = UUID.fromString(body.roomId)
          startTime = LocalDateTime.parse(body.startTime)
          endTime = LocalDateTime.parse(body.endTime)
          result <- bookingService.checkAvailability(roomId, startTime, endTime).attempt
          response <- result match {
            case Right(Right(isAvailable)) => Ok(isAvailable.toString)
            case Right(Left(error: ValidationError)) => BadRequest(ErrorResponse(error.message).asJson)
            case Left(error) => BadRequest(ErrorResponse(error.getMessage).asJson)
          }
        } yield response
    }
  }

  /**
   * Создает все HTTP роуты для API.
   * @param userService Сервис для работы с пользователями
   * @param roomService Сервис для работы с комнатами
   * @param bookingService Сервис для работы с бронированиями
   * @return HttpRoutes[IO] - маршруты API
   */
  def createRoutes(
    userService: UserService,
    roomService: RoomService,
    bookingService: BookingService
  ): HttpRoutes[IO] = {
    userRoutes(userService) <+>
    roomRoutes(roomService) <+>
    bookingRoutes(bookingService)
  }
} 