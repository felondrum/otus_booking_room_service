package ru.filippov.otus.roombooking.http

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s._
import org.http4s.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.filippov.otus.roombooking.model.{Booking, Room}
import ru.filippov.otus.roombooking.service.{BookingService, ValidationError}
import java.time.LocalDateTime
import java.util.UUID

class BookingControllerSpec extends AnyFlatSpec with Matchers {
  class MockBookingRepository {
    def create(roomId: UUID, userId: UUID, startTime: LocalDateTime, endTime: LocalDateTime): IO[Booking] =
      IO.pure(Booking(UUID.randomUUID(), roomId, userId, startTime, endTime, LocalDateTime.now()))
    def delete(id: UUID): IO[Unit] = IO.unit
    def getAll: IO[List[Booking]] = {
      val now = LocalDateTime.now()
      IO.pure(List(
        Booking(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), now, now.plusHours(1), now),
        Booking(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), now.plusHours(2), now.plusHours(3), now)
      ))
    }
    def checkAvailability(roomId: UUID, startTime: LocalDateTime, endTime: LocalDateTime): IO[Boolean] = IO.pure(true)
  }

  class MockRoomRepository {
    def create(name: String, capacity: Int, description: Option[String]): IO[Room] =
      IO.pure(Room(UUID.randomUUID(), name, capacity, description))
    def delete(id: UUID): IO[Unit] = IO.unit
    def getAll: IO[List[Room]] = IO.pure(List())
    def getAvailableRooms(startTime: LocalDateTime, endTime: LocalDateTime, capacity: Option[Int]): IO[List[Room]] = IO.pure(List())
    def getAvailableRoomsByDate(date: LocalDateTime, capacity: Option[Int]): IO[List[Room]] = IO.pure(List())
  }

  val mockBookingRepository = new MockBookingRepository
  val mockRoomRepository = new MockRoomRepository

  val mockBookingService = new BookingService(null, null) {
    override def createBooking(roomId: UUID, userId: UUID, startTime: LocalDateTime, endTime: LocalDateTime): IO[Either[ValidationError, Booking]] = {
      if (startTime.isBefore(endTime)) {
        mockBookingRepository.create(roomId, userId, startTime, endTime).map(Right(_))
      } else {
        IO.pure(Left(ValidationError("Start time must be before end time")))
      }
    }
    override def deleteBooking(id: UUID): IO[Unit] = mockBookingRepository.delete(id)
    override def getAllBookings: IO[List[Booking]] = mockBookingRepository.getAll
    override def checkAvailability(roomId: UUID, startTime: LocalDateTime, endTime: LocalDateTime): IO[Either[ValidationError, Boolean]] =
      mockBookingRepository.checkAvailability(roomId, startTime, endTime).map(Right(_))
  }

  val bookingRoutes = Controllers.bookingRoutes(mockBookingService)

  "BookingController" should "create a new booking with valid data" in {
    val request = Request[IO](
      method = Method.POST,
      uri = uri"/bookings"
    ).withEntity("""{
      "roomId": "123e4567-e89b-12d3-a456-426614174000",
      "userId": "123e4567-e89b-12d3-a456-426614174001",
      "startTime": "2024-03-20T10:00:00",
      "endTime": "2024-03-20T11:00:00"
    }""")

    val response = bookingRoutes.orNotFound(request).unsafeRunSync()
    response.status shouldBe Status.Ok
  }

  it should "return bad request for invalid time range" in {
    val request = Request[IO](
      method = Method.POST,
      uri = uri"/bookings"
    ).withEntity("""{
      "roomId": "123e4567-e89b-12d3-a456-426614174000",
      "userId": "123e4567-e89b-12d3-a456-426614174001",
      "startTime": "2024-03-20T11:00:00",
      "endTime": "2024-03-20T10:00:00"
    }""")

    val response = bookingRoutes.orNotFound(request).unsafeRunSync()
    response.status shouldBe Status.BadRequest
  }

  it should "return list of all bookings" in {
    val request = Request[IO](
      method = Method.GET,
      uri = uri"/bookings"
    )

    val response = bookingRoutes.orNotFound(request).unsafeRunSync()
    response.status shouldBe Status.Ok
  }

  it should "check room availability" in {
    val request = Request[IO](
      method = Method.GET,
      uri = uri"/bookings/check"
    ).withEntity("""{
      "roomId": "123e4567-e89b-12d3-a456-426614174000",
      "startTime": "2024-03-20T10:00:00",
      "endTime": "2024-03-20T11:00:00"
    }""")

    val response = bookingRoutes.orNotFound(request).unsafeRunSync()
    response.status shouldBe Status.Ok
  }
} 