package ru.filippov.otus.roombooking.service

import cats.effect.IO
import ru.filippov.otus.roombooking.model.{User, Room, Booking}
import ru.filippov.otus.roombooking.repository.{UserRepository, RoomRepository, BookingRepository}
import java.time.LocalDateTime
import java.util.UUID

class UserService(userRepository: UserRepository) {
  def createUser(name: String, email: String): IO[User] = {
    userRepository.create(User(name = name, email = email))
  }

  def getAllUsers: IO[List[User]] = {
    userRepository.getAll
  }
}

class RoomService(roomRepository: RoomRepository) {
  def createRoom(name: String, capacity: Int, description: Option[String]): IO[Room] = {
    roomRepository.create(Room(name = name, capacity = capacity, description = description))
  }

  def deleteRoom(id: UUID): IO[Unit] = {
    roomRepository.delete(id)
  }

  def getAllRooms: IO[List[Room]] = {
    roomRepository.getAll
  }

  def getAvailableRooms(startTime: LocalDateTime, endTime: LocalDateTime, capacity: Option[Int] = None): IO[List[Room]] = {
    roomRepository.getAvailableRooms(startTime, endTime, capacity)
  }

  def getAvailableRoomsByDate(date: LocalDateTime, capacity: Option[Int] = None): IO[List[Room]] = {
    roomRepository.getAvailableRoomsByDate(date, capacity)
  }
}

class BookingService(
  bookingRepository: BookingRepository,
  roomRepository: RoomRepository
) {
  def createBooking(roomId: UUID, userId: UUID, startTime: LocalDateTime, endTime: LocalDateTime): IO[Either[String, Booking]] = {
    for {
      isAvailable <- bookingRepository.isRoomAvailable(roomId, startTime, endTime)
      result <- if (isAvailable) {
        val booking = Booking(
          roomId = roomId,
          userId = userId,
          startTime = startTime,
          endTime = endTime,
          createdAt = LocalDateTime.now()
        )
        bookingRepository.create(booking).map(Right(_))
      } else {
        IO.pure(Left("Room is not available for the specified time period"))
      }
    } yield result
  }

  def deleteBooking(id: UUID): IO[Unit] = {
    bookingRepository.delete(id)
  }

  def getAllBookings: IO[List[Booking]] = {
    bookingRepository.getAll
  }

  def checkAvailability(roomId: UUID, startTime: LocalDateTime, endTime: LocalDateTime): IO[Boolean] = {
    bookingRepository.isRoomAvailable(roomId, startTime, endTime)
  }
} 