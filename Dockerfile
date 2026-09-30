# Estágio de build usando o próprio Wrapper do Gradle do projeto
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon

# Estágio de execução
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
EXPOSE 8080
COPY --from=build --chown=app:app /app/build/libs/*.jar app.jar
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
