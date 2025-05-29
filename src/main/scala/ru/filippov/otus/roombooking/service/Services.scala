package ru.filippov.otus.roombooking.service

import cats.effect.IO
import ru.filippov.otus.roombooking.model.{User, Room, Booking}
import ru.filippov.otus.roombooking.repository.{UserRepository, RoomRepository, BookingRepository}
import java.time.LocalDateTime
import java.util.UUID
import cats.syntax.either._

case class ValidationError(message: String) extends RuntimeException(message)

object Validation {
  def validateEmail(email: String): Either[ValidationError, String] = {
    val emailRegex = """^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$""".r
    if (emailRegex.matches(email)) Right(email)
    else Left(ValidationError("Некорректный формат email"))
  }

  def validateName(name: String): Either[ValidationError, String] = {
    if (name.trim.nonEmpty && name.length <= 100) Right(name)
    else Left(ValidationError("Имя должно быть непустым и не длиннее 100 символов"))
  }

  def validateCapacity(capacity: Int): Either[ValidationError, Int] = {
    if (capacity > 0 && capacity <= 1000) Right(capacity)
    else Left(ValidationError("Вместимость должна быть от 1 до 1000 человек"))
  }

  def validateDescription(description: Option[String]): Either[ValidationError, Option[String]] = {
    description match {
      case Some(desc) if desc.length > 500 => Left(ValidationError("Описание не должно быть длиннее 500 символов"))
      case _ => Right(description)
    }
  }

  def validateTimeRange(startTime: LocalDateTime, endTime: LocalDateTime): Either[ValidationError, (LocalDateTime, LocalDateTime)] = {
    if (startTime.isAfter(endTime)) {
      Left(ValidationError("Время начала должно быть раньше времени окончания"))
    } else if (startTime.isBefore(LocalDateTime.now())) {
      Left(ValidationError("Нельзя бронировать комнату в прошлом"))
    } else if (endTime.isAfter(startTime.plusDays(30))) {
      Left(ValidationError("Нельзя бронировать комнату более чем на 30 дней вперед"))
    } else {
      Right((startTime, endTime))
    }
  }

  def validateDate(date: LocalDateTime): Either[ValidationError, LocalDateTime] = {
    if (date.toLocalDate.isBefore(LocalDateTime.now().toLocalDate)) {
      Left(ValidationError("Нельзя искать доступные комнаты в прошлом"))
    } else if (date.toLocalDate.isAfter(LocalDateTime.now().plusDays(30).toLocalDate)) {
      Left(ValidationError("Нельзя искать доступные комнаты более чем на 30 дней вперед"))
    } else {
      Right(date)
    }
  }
}

class UserService(userRepository: UserRepository) {
  def createUser(name: String, email: String): IO[Either[ValidationError, User]] = {
    for {
      validatedName <- IO.fromEither(Validation.validateName(name))
      validatedEmail <- IO.fromEither(Validation.validateEmail(email))
      user <- userRepository.create(User(name = validatedName, email = validatedEmail)).map(Right(_))
    } yield user
  }

  def getAllUsers: IO[List[User]] = {
    userRepository.getAll
  }
}

class RoomService(roomRepository: RoomRepository) {
  def createRoom(name: String, capacity: Int, description: Option[String]): IO[Either[ValidationError, Room]] = {
    for {
      validatedName <- IO.fromEither(Validation.validateName(name))
      validatedCapacity <- IO.fromEither(Validation.validateCapacity(capacity))
      validatedDescription <- IO.fromEither(Validation.validateDescription(description))
      room <- roomRepository.create(Room(
        name = validatedName,
        capacity = validatedCapacity,
        description = validatedDescription
      )).map(Right(_))
    } yield room
  }

  def deleteRoom(id: UUID): IO[Unit] = {
    roomRepository.delete(id)
  }

  def getAllRooms: IO[List[Room]] = {
    roomRepository.getAll
  }

  def getAvailableRooms(startTime: LocalDateTime, endTime: LocalDateTime, capacity: Option[Int] = None): IO[Either[ValidationError, List[Room]]] = {
    for {
      validatedTimeRange <- IO.fromEither(Validation.validateTimeRange(startTime, endTime))
      _ <- capacity.map(c => IO.fromEither(Validation.validateCapacity(c))).getOrElse(IO.unit)
      rooms <- roomRepository.getAvailableRooms(validatedTimeRange._1, validatedTimeRange._2, capacity).map(Right(_))
    } yield rooms
  }

  def getAvailableRoomsByDate(date: LocalDateTime, capacity: Option[Int] = None): IO[Either[ValidationError, List[Room]]] = {
    for {
      validatedDate <- IO.fromEither(Validation.validateDate(date))
      _ <- capacity.map(c => IO.fromEither(Validation.validateCapacity(c))).getOrElse(IO.unit)
      rooms <- roomRepository.getAvailableRoomsByDate(validatedDate, capacity).map(Right(_))
    } yield rooms
  }
}

class BookingService(
  bookingRepository: BookingRepository,
  roomRepository: RoomRepository
) {
  def createBooking(roomId: UUID, userId: UUID, startTime: LocalDateTime, endTime: LocalDateTime): IO[Either[ValidationError, Booking]] = {
    for {
      validatedTimeRange <- IO.fromEither(Validation.validateTimeRange(startTime, endTime))
      isAvailable <- bookingRepository.isRoomAvailable(roomId, validatedTimeRange._1, validatedTimeRange._2)
      result <- if (isAvailable) {
        val booking = Booking(
          roomId = roomId,
          userId = userId,
          startTime = validatedTimeRange._1,
          endTime = validatedTimeRange._2,
          createdAt = LocalDateTime.now()
        )
        bookingRepository.create(booking).map(Right(_))
      } else {
        IO.pure(Left(ValidationError("Комната недоступна в указанный период времени")))
      }
    } yield result
  }

  def deleteBooking(id: UUID): IO[Unit] = {
    bookingRepository.delete(id)
  }

  def getAllBookings: IO[List[Booking]] = {
    bookingRepository.getAll
  }

  def checkAvailability(roomId: UUID, startTime: LocalDateTime, endTime: LocalDateTime): IO[Either[ValidationError, Boolean]] = {
    for {
      validatedTimeRange <- IO.fromEither(Validation.validateTimeRange(startTime, endTime))
      isAvailable <- bookingRepository.isRoomAvailable(roomId, validatedTimeRange._1, validatedTimeRange._2).map(Right(_))
    } yield isAvailable
  }
} 