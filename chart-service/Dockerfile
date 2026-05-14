# 🔹 Этап сборки
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# 🔹 Этап запуска
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/chart-service*.jar app.jar

# Порт приложения
EXPOSE 8080

# Запуск
ENTRYPOINT ["java", "-jar", "app.jar"]