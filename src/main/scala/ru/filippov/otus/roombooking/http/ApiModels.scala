package ru.filippov.otus.roombooking.http

/**
 * Запрос на создание нового пользователя
 * @param name Имя пользователя
 * @param email Email пользователя
 */
case class CreateUserRequest(name: String, email: String)

/**
 * Запрос на создание новой комнаты
 * @param name Название комнаты
 * @param capacity Вместимость комнаты
 * @param description Опциональное описание комнаты
 */
case class CreateRoomRequest(name: String, capacity: Int, description: Option[String])

/**
 * Запрос на создание нового бронирования
 * @param roomId ID комнаты
 * @param userId ID пользователя
 * @param startTime Время начала бронирования
 * @param endTime Время окончания бронирования
 */
case class CreateBookingRequest(roomId: String, userId: String, startTime: String, endTime: String)

/**
 * Запрос на проверку доступности комнаты
 * @param roomId ID комнаты
 * @param startTime Время начала проверки
 * @param endTime Время окончания проверки
 */
case class CheckAvailabilityRequest(roomId: String, startTime: String, endTime: String)

/**
 * Запрос на получение доступных комнат
 * @param startTime Время начала периода
 * @param endTime Время окончания периода
 * @param capacity Опциональная минимальная вместимость
 */
case class AvailableRoomsRequest(startTime: String, endTime: String, capacity: Option[Int] = None)

/**
 * Запрос на получение доступных комнат на конкретную дату
 * @param date Дата для проверки
 * @param capacity Опциональная минимальная вместимость
 */
case class AvailableRoomsByDateRequest(date: String, capacity: Option[Int] = None)

/**
 * Ответ с информацией о пользователе
 * @param id ID пользователя
 * @param name Имя пользователя
 * @param email Email пользователя
 */
case class UserResponse(id: String, name: String, email: String)

/**
 * Ответ с информацией о комнате
 * @param id ID комнаты
 * @param name Название комнаты
 * @param capacity Вместимость комнаты
 * @param description Описание комнаты
 */
case class RoomResponse(id: String, name: String, capacity: Int, description: Option[String])

/**
 * Ответ с информацией о бронировании
 * @param id ID бронирования
 * @param roomId ID комнаты
 * @param userId ID пользователя
 * @param startTime Время начала бронирования
 * @param endTime Время окончания бронирования
 * @param createdAt Время создания бронирования
 */
case class BookingResponse(
  id: String,
  roomId: String,
  userId: String,
  startTime: String,
  endTime: String,
  createdAt: String
)

/**
 * Ответ с информацией об ошибке
 * @param message Сообщение об ошибке
 */
case class ErrorResponse(message: String) 