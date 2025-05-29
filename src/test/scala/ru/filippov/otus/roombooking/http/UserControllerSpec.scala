package ru.filippov.otus.roombooking.http

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s._
import org.http4s.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.filippov.otus.roombooking.model.User
import ru.filippov.otus.roombooking.service.{UserService, ValidationError}
import java.util.UUID

class UserControllerSpec extends AnyFlatSpec with Matchers {
  class MockUserRepository {
    def create(name: String, email: String): IO[User] =
      IO.pure(User(UUID.randomUUID(), name, email))
    def getAll: IO[List[User]] =
      IO.pure(List(
        User(UUID.randomUUID(), "Test User 1", "test1@example.com"),
        User(UUID.randomUUID(), "Test User 2", "test2@example.com")
      ))
  }

  val mockUserRepository = new MockUserRepository

  val mockUserService = new UserService(null) {
    override def createUser(name: String, email: String): IO[Either[ValidationError, User]] = {
      if (email.contains("@")) {
        mockUserRepository.create(name, email).map(Right(_))
      } else {
        IO.pure(Left(ValidationError("Invalid email format")))
      }
    }
    override def getAllUsers: IO[List[User]] = mockUserRepository.getAll
  }

  val userRoutes = Controllers.userRoutes(mockUserService)

  "UserController" should "create a new user with valid data" in {
    val request = Request[IO](
      method = Method.POST,
      uri = uri"/users"
    ).withEntity("""{"name": "Test User", "email": "test@example.com"}""")

    val response = userRoutes.orNotFound(request).unsafeRunSync()
    response.status shouldBe Status.Ok
  }

  it should "return bad request for invalid email" in {
    val request = Request[IO](
      method = Method.POST,
      uri = uri"/users"
    ).withEntity("""{"name": "Test User", "email": "invalid-email"}""")

    val response = userRoutes.orNotFound(request).unsafeRunSync()
    response.status shouldBe Status.BadRequest
  }

  it should "return list of all users" in {
    val request = Request[IO](
      method = Method.GET,
      uri = uri"/users"
    )

    val response = userRoutes.orNotFound(request).unsafeRunSync()
    response.status shouldBe Status.Ok
  }
} 