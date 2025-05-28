#!/bin/bash

# Останавливаем и удаляем старые контейнеры
docker-compose down

# Собираем и запускаем контейнеры
docker-compose up --build 