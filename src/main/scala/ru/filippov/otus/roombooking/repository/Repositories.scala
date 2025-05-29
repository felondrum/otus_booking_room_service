package ru.filippov.otus.roombooking.repository

import cats.effect.IO
import ru.filippov.otus.roombooking.model.{User, Room, Booking}
import io.getquill.{PostgresJdbcContext, SnakeCase}
import java.time.LocalDateTime
import java.util.UUID

class UserRepository(ctx: PostgresJdbcContext[SnakeCase]) {
  import ctx.{IO => _, _}

  private val users = quote(querySchema[User]("users"))

  def create(user: User): IO[User] = IO {
    val q = quote {
      users.insert(_.name -> lift(user.name), _.email -> lift(user.email))
        .returning(_.id)
    }
    val id = run(q)
    user.copy(id = id)
  }

  def getAll: IO[List[User]] = IO {
    run(quote(users))
  }
}

class RoomRepository(ctx: PostgresJdbcContext[SnakeCase]) {
  import ctx.{IO => _, _}

  private val rooms = quote(querySchema[Room]("rooms"))
  private val bookings = quote(querySchema[Booking]("bookings"))

  def create(room: Room): IO[Room] = IO {
    val q = quote {
      rooms.insert(_.name -> lift(room.name), _.capacity -> lift(room.capacity), _.description -> lift(room.description))
        .returning(_.id)
    }
    val id = run(q)
    room.copy(id = id)
  }

  def delete(id: UUID): IO[Unit] = IO {
    run(quote(rooms.filter(_.id == lift(id)).delete))
  }

  def getAll: IO[List[Room]] = IO {
    run(quote(rooms))
  }

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

  def getAvailableRoomsByDate(date: LocalDateTime, capacity: Option[Int] = None): IO[List[Room]] = {
    val startOfDay = date.toLocalDate.atStartOfDay()
    val endOfDay = date.toLocalDate.plusDays(1).atStartOfDay()
    getAvailableRooms(startOfDay, endOfDay, capacity)
  }
}

class BookingRepository(ctx: PostgresJdbcContext[SnakeCase]) {
  import ctx.{IO => _, _}

  private val bookings = quote(querySchema[Booking]("bookings"))

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

  def delete(id: UUID): IO[Unit] = IO {
    run(quote(bookings.filter(_.id == lift(id)).delete))
  }

  def getAll: IO[List[Booking]] = IO {
    run(quote(bookings))
  }

  def isRoomAvailable(roomId: UUID, startTime: LocalDateTime, endTime: LocalDateTime): IO[Boolean] = IO {
    val conflictingBookings = run(quote(
      bookings.filter(b => 
        b.roomId == lift(roomId) &&
        infix"${b.startTime} < ${lift(endTime)} AND ${b.endTime} > ${lift(startTime)}".as[Boolean]
      )
    ))
    conflictingBookings.isEmpty
  }
} 