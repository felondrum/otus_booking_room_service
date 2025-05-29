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

object Controllers {
  // Request models
  case class CreateUserRequest(name: String, email: String)
  case class CreateRoomRequest(name: String, capacity: Int, description: Option[String])
  case class CreateBookingRequest(roomId: String, userId: String, startTime: String, endTime: String)
  case class CheckAvailabilityRequest(roomId: String, startTime: String, endTime: String)
  case class AvailableRoomsRequest(startTime: String, endTime: String, capacity: Option[Int] = None)
  case class AvailableRoomsByDateRequest(date: String, capacity: Option[Int] = None)

  // Response models
  case class UserResponse(id: String, name: String, email: String)
  case class RoomResponse(id: String, name: String, capacity: Int, description: Option[String])
  case class BookingResponse(
    id: String,
    roomId: String,
    userId: String,
    startTime: String,
    endTime: String,
    createdAt: String
  )
  case class ErrorResponse(message: String)

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
  private def toUserResponse(user: User): UserResponse = 
    UserResponse(user.id.toString, user.name, user.email)

  private def toRoomResponse(room: Room): RoomResponse = 
    RoomResponse(room.id.toString, room.name, room.capacity, room.description)

  private def toBookingResponse(booking: Booking): BookingResponse = 
    BookingResponse(
      booking.id.toString,
      booking.roomId.toString,
      booking.userId.toString,
      booking.startTime.toString,
      booking.endTime.toString,
      booking.createdAt.toString
    )

  def userRoutes(userService: UserService): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
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

      case GET -> Root / "users" =>
        for {
          _ <- IO.println("Received GET /users request")
          users <- userService.getAllUsers
          _ <- IO.println(s"Found users: $users")
          response <- Ok(users.map(toUserResponse).asJson)
        } yield response
    }
  }

  def roomRoutes(roomService: RoomService): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
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

      case DELETE -> Root / "rooms" / UUIDVar(id) =>
        for {
          _ <- IO.println(s"Received DELETE /rooms/$id request")
          _ <- roomService.deleteRoom(id)
          _ <- IO.println(s"Deleted room: $id")
          response <- Ok()
        } yield response

      case GET -> Root / "rooms" =>
        for {
          _ <- IO.println("Received GET /rooms request")
          rooms <- roomService.getAllRooms
          _ <- IO.println(s"Found rooms: $rooms")
          response <- Ok(rooms.map(toRoomResponse).asJson)
        } yield response

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

  def bookingRoutes(bookingService: BookingService): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
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

      case DELETE -> Root / "bookings" / UUIDVar(id) =>
        for {
          _ <- IO.println(s"Received DELETE /bookings/$id request")
          _ <- bookingService.deleteBooking(id)
          _ <- IO.println(s"Deleted booking: $id")
          response <- Ok()
        } yield response

      case GET -> Root / "bookings" =>
        for {
          _ <- IO.println("Received GET /bookings request")
          bookings <- bookingService.getAllBookings
          _ <- IO.println(s"Found bookings: $bookings")
          response <- Ok(bookings.map(toBookingResponse).asJson)
        } yield response

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
} 