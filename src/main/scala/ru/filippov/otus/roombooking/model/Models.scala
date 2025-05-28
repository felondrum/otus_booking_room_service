package ru.filippov.otus.roombooking.model

import java.time.LocalDateTime
import java.util.UUID

case class User(
  id: UUID = UUID.randomUUID(),
  name: String,
  email: String
)

case class Room(
  id: UUID = UUID.randomUUID(),
  name: String,
  capacity: Int,
  description: Option[String]
)

case class Booking(
  id: UUID = UUID.randomUUID(),
  roomId: UUID,
  userId: UUID,
  startTime: LocalDateTime,
  endTime: LocalDateTime,
  createdAt: LocalDateTime = LocalDateTime.now()
) 