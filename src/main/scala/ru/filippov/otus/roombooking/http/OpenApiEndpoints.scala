package ru.filippov.otus.roombooking.http

import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import io.circe.generic.auto._
import java.util.UUID
import cats.effect.IO
import sttp.model.StatusCode

/**
 * Объект, содержащий определения OpenAPI эндпоинтов
 */
object OpenApiEndpoints {
  // Базовые пути
  private val basePath = "api"
  private val usersPath = basePath / "users"
  private val roomsPath = basePath / "rooms"
  private val bookingsPath = basePath / "bookings"

  // Общие схемы ошибок
  private val errorResponse = oneOf[ErrorResponse](
    oneOfVariant(StatusCode.BadRequest, jsonBody[ErrorResponse]
      .description("Ошибка валидации")
      .example(ErrorResponse("Некорректный формат email")))
  )

  // Эндпоинты пользователей
  val createUserEndpoint = endpoint.post
    .in(usersPath)
    .in(jsonBody[CreateUserRequest]
      .description("Данные для создания пользователя")
      .example(CreateUserRequest("Иван Иванов", "ivan@example.com"))
      .validate(
        Validator.nonEmptyString.contramap[CreateUserRequest](_.name)
          .and(Validator.maxLength(100).contramap[CreateUserRequest](_.name))
      )
      .validate(Validator.pattern("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$").contramap[CreateUserRequest](_.email)))
    .out(jsonBody[UserResponse]
      .description("Созданный пользователь")
      .example(UserResponse("550e8400-e29b-41d4-a716-446655440000", "Иван Иванов", "ivan@example.com")))
    .errorOut(errorResponse)
    .tag("Пользователи")
    .description("Создание нового пользователя")
    .name("createUser")

  val getAllUsersEndpoint = endpoint.get
    .in(usersPath)
    .out(jsonBody[List[UserResponse]]
      .description("Список пользователей")
      .example(List(
        UserResponse("550e8400-e29b-41d4-a716-446655440000", "Иван Иванов", "ivan@example.com"),
        UserResponse("550e8400-e29b-41d4-a716-446655440001", "Петр Петров", "petr@example.com")
      )))
    .tag("Пользователи")
    .description("Получение списка всех пользователей")
    .name("getAllUsers")

  // Эндпоинты комнат
  val createRoomEndpoint = endpoint.post
    .in(roomsPath)
    .in(jsonBody[CreateRoomRequest]
      .description("Данные для создания комнаты")
      .example(CreateRoomRequest("Конференц-зал", 10, Some("Большой конференц-зал на 10 человек")))
      .validate(
        Validator.nonEmptyString.contramap[CreateRoomRequest](_.name)
          .and(Validator.maxLength(100).contramap[CreateRoomRequest](_.name))
      )
      .validate(Validator.min(1).and(Validator.max(1000)).contramap[CreateRoomRequest](_.capacity)))
    .out(jsonBody[RoomResponse]
      .description("Созданная комната")
      .example(RoomResponse("550e8400-e29b-41d4-a716-446655440000", "Конференц-зал", 10, Some("Большой конференц-зал на 10 человек"))))
    .errorOut(errorResponse)
    .tag("Комнаты")
    .description("Создание новой комнаты")
    .name("createRoom")

  val deleteRoomEndpoint = endpoint.delete
    .in(roomsPath / path[UUID]("id").description("ID комнаты"))
    .out(statusCode(StatusCode.NoContent))
    .tag("Комнаты")
    .description("Удаление комнаты")
    .name("deleteRoom")

  val getAllRoomsEndpoint = endpoint.get
    .in(roomsPath)
    .out(jsonBody[List[RoomResponse]]
      .description("Список комнат")
      .example(List(
        RoomResponse("550e8400-e29b-41d4-a716-446655440000", "Конференц-зал", 10, Some("Большой конференц-зал на 10 человек")),
        RoomResponse("550e8400-e29b-41d4-a716-446655440001", "Переговорная", 4, Some("Малая переговорная на 4 человека"))
      )))
    .tag("Комнаты")
    .description("Получение списка всех комнат")
    .name("getAllRooms")

  val getAvailableRoomsEndpoint = endpoint.post
    .in(roomsPath / "available")
    .in(jsonBody[AvailableRoomsRequest]
      .description("Параметры поиска доступных комнат")
      .example(AvailableRoomsRequest(
        "2024-03-20T10:00:00",
        "2024-03-20T12:00:00",
        Some(5)
      ))
      .validate(Validator.min(1).and(Validator.max(1000)).contramap[AvailableRoomsRequest](_.capacity.getOrElse(1))))
    .out(jsonBody[List[RoomResponse]]
      .description("Список доступных комнат")
      .example(List(
        RoomResponse("550e8400-e29b-41d4-a716-446655440000", "Конференц-зал", 10, Some("Большой конференц-зал на 10 человек"))
      )))
    .errorOut(errorResponse)
    .tag("Комнаты")
    .description("Поиск доступных комнат на указанный период")
    .name("getAvailableRooms")

  val getAvailableRoomsByDateEndpoint = endpoint.post
    .in(roomsPath / "available" / "date")
    .in(jsonBody[AvailableRoomsByDateRequest]
      .description("Параметры поиска доступных комнат")
      .example(AvailableRoomsByDateRequest(
        "2024-03-20",
        Some(5)
      ))
      .validate(Validator.min(1).and(Validator.max(1000)).contramap[AvailableRoomsByDateRequest](_.capacity.getOrElse(1))))
    .out(jsonBody[List[RoomResponse]]
      .description("Список доступных комнат")
      .example(List(
        RoomResponse("550e8400-e29b-41d4-a716-446655440000", "Конференц-зал", 10, Some("Большой конференц-зал на 10 человек"))
      )))
    .errorOut(errorResponse)
    .tag("Комнаты")
    .description("Поиск доступных комнат на указанную дату")
    .name("getAvailableRoomsByDate")

  // Эндпоинты бронирований
  val createBookingEndpoint = endpoint.post
    .in(bookingsPath)
    .in(jsonBody[CreateBookingRequest]
      .description("Данные для создания бронирования")
      .example(CreateBookingRequest(
        "550e8400-e29b-41d4-a716-446655440000",
        "550e8400-e29b-41d4-a716-446655440001",
        "2024-03-20T10:00:00",
        "2024-03-20T12:00:00"
      )))
    .out(jsonBody[BookingResponse]
      .description("Созданное бронирование")
      .example(BookingResponse(
        "550e8400-e29b-41d4-a716-446655440002",
        "550e8400-e29b-41d4-a716-446655440000",
        "550e8400-e29b-41d4-a716-446655440001",
        "2024-03-20T10:00:00",
        "2024-03-20T12:00:00",
        "2024-03-19T15:30:00"
      )))
    .errorOut(errorResponse)
    .tag("Бронирования")
    .description("Создание нового бронирования")
    .name("createBooking")

  val deleteBookingEndpoint = endpoint.delete
    .in(bookingsPath / path[UUID]("id").description("ID бронирования"))
    .out(statusCode(StatusCode.NoContent))
    .tag("Бронирования")
    .description("Удаление бронирования")
    .name("deleteBooking")

  val getAllBookingsEndpoint = endpoint.get
    .in(bookingsPath)
    .out(jsonBody[List[BookingResponse]]
      .description("Список бронирований")
      .example(List(
        BookingResponse(
          "550e8400-e29b-41d4-a716-446655440002",
          "550e8400-e29b-41d4-a716-446655440000",
          "550e8400-e29b-41d4-a716-446655440001",
          "2024-03-20T10:00:00",
          "2024-03-20T12:00:00",
          "2024-03-19T15:30:00"
        )
      )))
    .tag("Бронирования")
    .description("Получение списка всех бронирований")
    .name("getAllBookings")

  val getBookingsByUserIdEndpoint = endpoint.get
    .in(bookingsPath / "user" / path[UUID]("userId").description("ID пользователя"))
    .out(jsonBody[List[BookingResponse]]
      .description("Список бронирований пользователя")
      .example(List(
        BookingResponse(
          "550e8400-e29b-41d4-a716-446655440002",
          "550e8400-e29b-41d4-a716-446655440000",
          "550e8400-e29b-41d4-a716-446655440001",
          "2024-03-20T10:00:00",
          "2024-03-20T12:00:00",
          "2024-03-19T15:30:00"
        )
      )))
    .tag("Бронирования")
    .description("Получение списка бронирований пользователя")
    .name("getBookingsByUserId")

  val checkAvailabilityEndpoint = endpoint.get
    .in(bookingsPath / "check")
    .in(jsonBody[CheckAvailabilityRequest]
      .description("Параметры проверки доступности")
      .example(CheckAvailabilityRequest(
        "550e8400-e29b-41d4-a716-446655440000",
        "2024-03-20T10:00:00",
        "2024-03-20T12:00:00"
      )))
    .out(jsonBody[Boolean]
      .description("Результат проверки доступности")
      .example(true))
    .errorOut(errorResponse)
    .tag("Бронирования")
    .description("Проверка доступности комнаты на указанный период")
    .name("checkAvailability")

  // Все эндпоинты
  val allEndpoints = List(
    createUserEndpoint,
    getAllUsersEndpoint,
    createRoomEndpoint,
    deleteRoomEndpoint,
    getAllRoomsEndpoint,
    getAvailableRoomsEndpoint,
    getAvailableRoomsByDateEndpoint,
    createBookingEndpoint,
    deleteBookingEndpoint,
    getAllBookingsEndpoint,
    getBookingsByUserIdEndpoint,
    checkAvailabilityEndpoint
  )

  // Swagger UI
  val swaggerEndpoints = SwaggerInterpreter()
    .fromEndpoints[IO](allEndpoints, "OTUS. Сервис бронирования комнат", "1.0.0")
} 