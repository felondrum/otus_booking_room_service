FROM openjdk:17-slim as builder

WORKDIR /app

# Установка необходимых пакетов
RUN apt-get update && apt-get install -y \
    bash \
    curl \
    && curl -L -o sbt.deb https://repo.scala-sbt.org/scalasbt/debian/sbt-1.9.8.deb \
    && dpkg -i sbt.deb || true \
    && apt-get install -f -y \
    && rm sbt.deb

# Настройка SBT
RUN mkdir -p ~/.sbt/1.0/plugins
RUN echo 'resolvers += Resolver.sbtPluginRepo("releases")' > ~/.sbt/1.0/plugins/build.sbt
RUN echo 'resolvers += Resolver.typesafeRepo("releases")' >> ~/.sbt/1.0/plugins/build.sbt

COPY . .
RUN sbt stage

FROM openjdk:17-slim

WORKDIR /app
COPY --from=builder /app/target/universal/stage /app
COPY --from=builder /app/src/main/resources /app/conf

EXPOSE 8080
CMD ["./bin/room-booking-service"]
