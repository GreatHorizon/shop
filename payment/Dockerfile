# =============================================================================
# Stage 1: Build
# =============================================================================
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build
# Копируем только pom.xml сначала для лучшего использования кэша слоёв
COPY pom.xml .
# Скачиваем зависимости (кэшируется отдельно от кода)
# Кэшируется, если pom.xml не менялся
RUN mvn dependency:go-offline -B
# Копируем исходный код
# Пересобирается только при изменении кода
COPY src src
# Собираем приложение
RUN mvn clean package -DskipTests -B
# =============================================================================
# Stage 2: Runtime
# =============================================================================
FROM eclipse-temurin:21-jre-jammy
# Создаём non-root пользователя для безопасности
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
WORKDIR /app
# Копируем только JAR-файл из builder stage
# Используем конкретное имя вместо *.jar для предсказуемости
COPY --from=builder /build/target/payment-0.0.1-SNAPSHOT.jar app.jar
# Меняем владельца на non-root пользователя
RUN chown appuser:appgroup app.jar
# Переключаемся на non-root пользователя
USER appuser
# Открываем порт
EXPOSE 8081
# JVM оптимизации для контейнеров
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
# Запуск приложения
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]