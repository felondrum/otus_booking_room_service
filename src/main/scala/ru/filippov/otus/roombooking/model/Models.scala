package ru.filippov.otus.roombooking.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * Модель пользователя системы бронирования
 * @param id Уникальный идентификатор пользователя
 * @param name Имя пользователя
 * @param email Email пользователя
 */
case class User(
  id: UUID = UUID.randomUUID(),
  name: String,
  email: String
)

/**
 * Модель комнаты для бронирования
 * @param id Уникальный идентификатор комнаты
 * @param name Название комнаты
 * @param capacity Вместимость комнаты (количество человек)
 * @param description Опциональное описание комнаты
 */
case class Room(
  id: UUID = UUID.randomUUID(),
  name: String,
  capacity: Int,
  description: Option[String]
)

/**
 * Модель бронирования комнаты
 * @param id Уникальный идентификатор бронирования
 * @param roomId Идентификатор забронированной комнаты
 * @param userId Идентификатор пользователя, сделавшего бронирование
 * @param startTime Время начала бронирования
 * @param endTime Время окончания бронирования
 * @param createdAt Время создания бронирования
 */
case class Booking(
  id: UUID = UUID.randomUUID(),
  roomId: UUID,
  userId: UUID,
  startTime: LocalDateTime,
  endTime: LocalDateTime,
  createdAt: LocalDateTime = LocalDateTime.now()
) 