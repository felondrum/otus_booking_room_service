package ru.filippov.otus.roombooking.repository

import cats.effect.IO
import ru.filippov.otus.roombooking.model.{User, Room, Booking}
import io.getquill.{PostgresJdbcContext, SnakeCase}
import java.time.LocalDateTime
import java.util.UUID

/**
 * Репозиторий для работы с пользователями в базе данных
 * @param ctx Контекст базы данных PostgreSQL
 */
class UserRepository(ctx: PostgresJdbcContext[SnakeCase]) {
  import ctx.{IO => _, _}

  private val users = quote(querySchema[User]("users"))

  /**
   * Создает нового пользователя в базе данных
   * @param user Модель пользователя для создания
   * @return IO[User] - созданный пользователь с присвоенным ID
   */
  def create(user: User): IO[User] = IO {
    val q = quote {
      users.insert(_.name -> lift(user.name), _.email -> lift(user.email))
        .returning(_.id)
    }
    val id = run(q)
    user.copy(id = id)
  }

  /**
   * Получает список всех пользователей из базы данных
   * @return IO[List[User]] - список пользователей
   */
  def getAll: IO[List[User]] = IO {
    run(quote(users))
  }
}

/**
 * Репозиторий для работы с комнатами в базе данных
 * @param ctx Контекст базы данных PostgreSQL
 */
class RoomRepository(ctx: PostgresJdbcContext[SnakeCase]) {
  import ctx.{IO => _, _}

  private val rooms = quote(querySchema[Room]("rooms"))
  private val bookings = quote(querySchema[Booking]("bookings"))

  /**
   * Создает новую комнату в базе данных
   * @param room Модель комнаты для создания
   * @return IO[Room] - созданная комната с присвоенным ID
   */
  def create(room: Room): IO[Room] = IO {
    val q = quote {
      rooms.insert(_.name -> lift(room.name), _.capacity -> lift(room.capacity), _.description -> lift(room.description))
        .returning(_.id)
    }
    val id = run(q)
    room.copy(id = id)
  }

  /**
   * Удаляет комнату из базы данных по ID
   * @param id ID комнаты для удаления
   * @return IO[Unit] - результат удаления
   */
  def delete(id: UUID): IO[Unit] = IO {
    run(quote(rooms.filter(_.id == lift(id)).delete))
  }

  /**
   * Получает список всех комнат из базы данных
   * @return IO[List[Room]] - список комнат
   */
  def getAll: IO[List[Room]] = IO {
    run(quote(rooms))
  }

  /**
   * Получает список доступных комнат на указанный период
   * @param startTime Время начала периода
   * @param endTime Время окончания периода
   * @param capacity Опциональная минимальная вместимость
   * @return IO[List[Room]] - список доступных комнат
   */
  def getAvailableRooms(startTime: LocalDateTime, endTime: LocalDateTime, capacity: Option[Int] = None): IO[List[Room]] = IO {
    val query = quote {
      rooms.filter(room =>
        !bookings.filter(booking =>
          booking.roomId == room.id &&
          infix"${booking.startTime} < ${lift(endTime)} AND ${booking.endTime} > ${lift(startTime)}".as[Boolean]
        ).nonEmpty &&
        lift(capacity).forall(c => room.capacity >= c)
      )
    }
    run(query)
  }

  /**
   * Получает список доступных комнат на указанную дату
   * @param date Дата для проверки
   * @param capacity Опциональная минимальная вместимость
   * @return IO[List[Room]] - список доступных комнат
   */
  def getAvailableRoomsByDate(date: LocalDateTime, capacity: Option[Int] = None): IO[List[Room]] = {
    val startOfDay = date.toLocalDate.atStartOfDay()
    val endOfDay = date.toLocalDate.plusDays(1).atStartOfDay()
    getAvailableRooms(startOfDay, endOfDay, capacity)
  }
}

/**
 * Репозиторий для работы с бронированиями в базе данных
 * @param ctx Контекст базы данных PostgreSQL
 */
class BookingRepository(ctx: PostgresJdbcContext[SnakeCase]) {
  import ctx.{IO => _, _}

  private val bookings = quote(querySchema[Booking]("bookings"))

  /**
   * Создает новое бронирование в базе данных
   * @param booking Модель бронирования для создания
   * @return IO[Booking] - созданное бронирование с присвоенным ID
   */
  def create(booking: Booking): IO[Booking] = IO {
    val q = quote {
      bookings.insert(
        _.roomId -> lift(booking.roomId),
        _.userId -> lift(booking.userId),
        _.startTime -> lift(booking.startTime),
        _.endTime -> lift(booking.endTime),
        _.createdAt -> lift(booking.createdAt)
      ).returning(_.id)
    }
    val id = run(q)
    booking.copy(id = id)
  }

  /**
   * Удаляет бронирование из базы данных по ID
   * @param id ID бронирования для удаления
   * @return IO[Unit] - результат удаления
   */
  def delete(id: UUID): IO[Unit] = IO {
    run(quote(bookings.filter(_.id == lift(id)).delete))
  }

  /**
   * Получает список всех бронирований из базы данных
   * @return IO[List[Booking]] - список бронирований
   */
  def getAll: IO[List[Booking]] = IO {
    run(quote(bookings))
  }

  /**
   * Проверяет доступность комнаты на указанный период
   * @param roomId ID комнаты
   * @param startTime Время начала периода
   * @param endTime Время окончания периода
   * @return IO[Boolean] - true если комната доступна, false в противном случае
   */
  def isRoomAvailable(roomId: UUID, startTime: LocalDateTime, endTime: LocalDateTime): IO[Boolean] = IO {
    val conflictingBookings = run(quote(
      bookings.filter(b => 
        b.roomId == lift(roomId) &&
        infix"${b.startTime} < ${lift(endTime)} AND ${b.endTime} > ${lift(startTime)}".as[Boolean]
      )
    ))
    conflictingBookings.isEmpty
  }

  /**
   * Получает список бронирований по ID пользователя
   * @param userId ID пользователя
   * @return IO[List[Booking]] - список бронирований пользователя
   */
  def getBookingsByUserId(userId: UUID): IO[List[Booking]] = IO {
    run(quote(bookings.filter(_.userId == lift(userId))))
  }
} 