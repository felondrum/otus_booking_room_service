package ru.filippov.otus.roombooking.service

import cats.effect.IO
import ru.filippov.otus.roombooking.model.{User, Room, Booking}
import ru.filippov.otus.roombooking.repository.{UserRepository, RoomRepository, BookingRepository}
import java.time.LocalDateTime
import java.util.UUID
import cats.syntax.either._

/**
 * Ошибка валидации данных
 * @param message Сообщение об ошибке
 */
case class ValidationError(message: String) extends RuntimeException(message)

/**
 * Объект, содержащий методы валидации данных
 */
object Validation {
  /**
   * Валидирует email пользователя
   * @param email Email для проверки
   * @return Right(email) если email валиден, Left(ValidationError) в противном случае
   */
  def validateEmail(email: String): Either[ValidationError, String] = {
    val emailRegex = """^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$""".r
    if (emailRegex.matches(email)) Right(email)
    else Left(ValidationError("Некорректный формат email"))
  }

  /**
   * Валидирует имя пользователя или название комнаты
   * @param name Имя для проверки
   * @return Right(name) если имя валидно, Left(ValidationError) в противном случае
   */
  def validateName(name: String): Either[ValidationError, String] = {
    if (name.trim.nonEmpty && name.length <= 100) Right(name)
    else Left(ValidationError("Имя должно быть непустым и не длиннее 100 символов"))
  }

  /**
   * Валидирует вместимость комнаты
   * @param capacity Вместимость для проверки
   * @return Right(capacity) если вместимость валидна, Left(ValidationError) в противном случае
   */
  def validateCapacity(capacity: Int): Either[ValidationError, Int] = {
    if (capacity > 0 && capacity <= 1000) Right(capacity)
    else Left(ValidationError("Вместимость должна быть от 1 до 1000 человек"))
  }

  /**
   * Валидирует описание комнаты
   * @param description Описание для проверки
   * @return Right(description) если описание валидно, Left(ValidationError) в противном случае
   */
  def validateDescription(description: Option[String]): Either[ValidationError, Option[String]] = {
    description match {
      case Some(desc) if desc.length > 500 => Left(ValidationError("Описание не должно быть длиннее 500 символов"))
      case _ => Right(description)
    }
  }

  /**
   * Валидирует временной диапазон для бронирования
   * @param startTime Время начала
   * @param endTime Время окончания
   * @return Right((startTime, endTime)) если диапазон валиден, Left(ValidationError) в противном случае
   */
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

  /**
   * Валидирует дату для поиска доступных комнат
   * @param date Дата для проверки
   * @return Right(date) если дата валидна, Left(ValidationError) в противном случае
   */
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

/**
 * Сервис для работы с пользователями
 * @param userRepository Репозиторий пользователей
 */
class UserService(userRepository: UserRepository) {
  /**
   * Создает нового пользователя
   * @param name Имя пользователя
   * @param email Email пользователя
   * @return IO[Either[ValidationError, User]] - результат создания пользователя
   */
  def createUser(name: String, email: String): IO[Either[ValidationError, User]] = {
    for {
      validatedName <- IO.fromEither(Validation.validateName(name))
      validatedEmail <- IO.fromEither(Validation.validateEmail(email))
      user <- userRepository.create(User(name = validatedName, email = validatedEmail)).map(Right(_))
    } yield user
  }

  /**
   * Получает список всех пользователей
   * @return IO[List[User]] - список пользователей
   */
  def getAllUsers: IO[List[User]] = {
    userRepository.getAll
  }
}

/**
 * Сервис для работы с комнатами
 * @param roomRepository Репозиторий комнат
 */
class RoomService(roomRepository: RoomRepository) {
  /**
   * Создает новую комнату
   * @param name Название комнаты
   * @param capacity Вместимость комнаты
   * @param description Опциональное описание комнаты
   * @return IO[Either[ValidationError, Room]] - результат создания комнаты
   */
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

  /**
   * Удаляет комнату по ID
   * @param id ID комнаты
   * @return IO[Unit] - результат удаления
   */
  def deleteRoom(id: UUID): IO[Unit] = {
    roomRepository.delete(id)
  }

  /**
   * Получает список всех комнат
   * @return IO[List[Room]] - список комнат
   */
  def getAllRooms: IO[List[Room]] = {
    roomRepository.getAll
  }

  /**
   * Получает список доступных комнат на указанный период
   * @param startTime Время начала периода
   * @param endTime Время окончания периода
   * @param capacity Опциональная минимальная вместимость
   * @return IO[Either[ValidationError, List[Room]]] - список доступных комнат
   */
  def getAvailableRooms(startTime: LocalDateTime, endTime: LocalDateTime, capacity: Option[Int] = None): IO[Either[ValidationError, List[Room]]] = {
    for {
      validatedTimeRange <- IO.fromEither(Validation.validateTimeRange(startTime, endTime))
      _ <- capacity.map(c => IO.fromEither(Validation.validateCapacity(c))).getOrElse(IO.unit)
      rooms <- roomRepository.getAvailableRooms(validatedTimeRange._1, validatedTimeRange._2, capacity).map(Right(_))
    } yield rooms
  }

  /**
   * Получает список доступных комнат на указанную дату
   * @param date Дата для проверки
   * @param capacity Опциональная минимальная вместимость
   * @return IO[Either[ValidationError, List[Room]]] - список доступных комнат
   */
  def getAvailableRoomsByDate(date: LocalDateTime, capacity: Option[Int] = None): IO[Either[ValidationError, List[Room]]] = {
    for {
      validatedDate <- IO.fromEither(Validation.validateDate(date))
      _ <- capacity.map(c => IO.fromEither(Validation.validateCapacity(c))).getOrElse(IO.unit)
      rooms <- roomRepository.getAvailableRoomsByDate(validatedDate, capacity).map(Right(_))
    } yield rooms
  }
}

/**
 * Сервис для работы с бронированиями
 * @param bookingRepository Репозиторий бронирований
 * @param roomRepository Репозиторий комнат
 */
class BookingService(
  bookingRepository: BookingRepository,
  roomRepository: RoomRepository
) {
  /**
   * Создает новое бронирование
   * @param roomId ID комнаты
   * @param userId ID пользователя
   * @param startTime Время начала бронирования
   * @param endTime Время окончания бронирования
   * @return IO[Either[ValidationError, Booking]] - результат создания бронирования
   */
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

  /**
   * Удаляет бронирование по ID
   * @param id ID бронирования
   * @return IO[Unit] - результат удаления
   */
  def deleteBooking(id: UUID): IO[Unit] = {
    bookingRepository.delete(id)
  }

  /**
   * Получает список всех бронирований
   * @return IO[List[Booking]] - список бронирований
   */
  def getAllBookings: IO[List[Booking]] = {
    bookingRepository.getAll
  }

  /**
   * Проверяет доступность комнаты на указанный период
   * @param roomId ID комнаты
   * @param startTime Время начала периода
   * @param endTime Время окончания периода
   * @return IO[Either[ValidationError, Boolean]] - результат проверки доступности
   */
  def checkAvailability(roomId: UUID, startTime: LocalDateTime, endTime: LocalDateTime): IO[Either[ValidationError, Boolean]] = {
    for {
      validatedTimeRange <- IO.fromEither(Validation.validateTimeRange(startTime, endTime))
      isAvailable <- bookingRepository.isRoomAvailable(roomId, validatedTimeRange._1, validatedTimeRange._2).map(Right(_))
    } yield isAvailable
  }
} 