FROM gradle:8.5-jdk17 AS build

# Устанавливаем рабочую директорию внутри контейнера
WORKDIR /app

# Копируем файлы Gradle (чтобы использовать кэш зависимостей Docker)
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle


RUN ./gradlew build --no-daemon -x test # Собираем приложение, пропуская тесты

# Копируем исходный код приложения
COPY src ./src

# Повторно собираем приложение (теперь с исходниками, используя кэшированные зависимости)
RUN ./gradlew build --no-daemon -x test

FROM eclipse-temurin:17-jre-focal

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем собранный JAR-файл из этапа сборки
COPY --from=build /app/build/libs/*.jar app.jar

# Указываем порт, который приложение будет слушать внутри контейнера
EXPOSE 8080

# Команда для запуска приложения при старте контейнера
ENTRYPOINT ["java", "-jar", "app.jar"]