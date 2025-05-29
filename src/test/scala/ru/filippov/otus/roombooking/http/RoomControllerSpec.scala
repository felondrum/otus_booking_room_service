package ru.filippov.otus.roombooking.http

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s._
import org.http4s.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.filippov.otus.roombooking.model.Room
import ru.filippov.otus.roombooking.service.{RoomService, ValidationError}
import java.time.LocalDateTime
import java.util.UUID

class RoomControllerSpec extends AnyFlatSpec with Matchers {
  class MockRoomRepository {
    def create(name: String, capacity: Int, description: Option[String]): IO[Room] =
      IO.pure(Room(UUID.randomUUID(), name, capacity, description))
    def delete(id: UUID): IO[Unit] = IO.unit
    def getAll: IO[List[Room]] =
      IO.pure(List(
        Room(UUID.randomUUID(), "Room 1", 10, Some("Test Room 1")),
        Room(UUID.randomUUID(), "Room 2", 20, Some("Test Room 2"))
      ))
    def getAvailableRooms(startTime: LocalDateTime, endTime: LocalDateTime, capacity: Option[Int]): IO[List[Room]] =
      IO.pure(List(
        Room(UUID.randomUUID(), "Available Room 1", 10, Some("Test Room 1")),
        Room(UUID.randomUUID(), "Available Room 2", 20, Some("Test Room 2"))
      ))
    def getAvailableRoomsByDate(date: LocalDateTime, capacity: Option[Int]): IO[List[Room]] =
      IO.pure(List(
        Room(UUID.randomUUID(), "Available Room 1", 10, Some("Test Room 1")),
        Room(UUID.randomUUID(), "Available Room 2", 20, Some("Test Room 2"))
      ))
  }

  val mockRoomRepository = new MockRoomRepository

  val mockRoomService = new RoomService(null) {
    override def createRoom(name: String, capacity: Int, description: Option[String]): IO[Either[ValidationError, Room]] = {
      if (capacity > 0) {
        mockRoomRepository.create(name, capacity, description).map(Right(_))
      } else {
        IO.pure(Left(ValidationError("Capacity must be positive")))
      }
    }
    override def deleteRoom(id: UUID): IO[Unit] = mockRoomRepository.delete(id)
    override def getAllRooms: IO[List[Room]] = mockRoomRepository.getAll
    override def getAvailableRooms(startTime: LocalDateTime, endTime: LocalDateTime, capacity: Option[Int]): IO[Either[ValidationError, List[Room]]] =
      mockRoomRepository.getAvailableRooms(startTime, endTime, capacity).map(Right(_))
    override def getAvailableRoomsByDate(date: LocalDateTime, capacity: Option[Int]): IO[Either[ValidationError, List[Room]]] =
      mockRoomRepository.getAvailableRoomsByDate(date, capacity).map(Right(_))
  }

  val roomRoutes = Controllers.roomRoutes(mockRoomService)

  "RoomController" should "create a new room with valid data" in {
    val request = Request[IO](
      method = Method.POST,
      uri = uri"/rooms"
    ).withEntity("""{"name": "Test Room", "capacity": 10, "description": "Test Description"}""")

    val response = roomRoutes.orNotFound(request).unsafeRunSync()
    response.status shouldBe Status.Ok
  }

  it should "return bad request for invalid capacity" in {
    val request = Request[IO](
      method = Method.POST,
      uri = uri"/rooms"
    ).withEntity("""{"name": "Test Room", "capacity": -1, "description": "Test Description"}""")

    val response = roomRoutes.orNotFound(request).unsafeRunSync()
    response.status shouldBe Status.BadRequest
  }

  it should "return list of all rooms" in {
    val request = Request[IO](
      method = Method.GET,
      uri = uri"/rooms"
    )

    val response = roomRoutes.orNotFound(request).unsafeRunSync()
    response.status shouldBe Status.Ok
  }

  it should "find available rooms for time period" in {
    val request = Request[IO](
      method = Method.POST,
      uri = uri"/rooms/available"
    ).withEntity("""{"startTime": "2024-03-20T10:00:00", "endTime": "2024-03-20T11:00:00", "capacity": 10}""")

    val response = roomRoutes.orNotFound(request).unsafeRunSync()
    response.status shouldBe Status.Ok
  }
} 