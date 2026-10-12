# ---- Build stage ----
# Usa la imagen oficial de Gradle (evita problemas de permisos/CRLF con ./gradlew)
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY --chown=gradle:gradle . .
# bootJar no corre los tests -> build más rápido y no bloquea el deploy
RUN gradle clean bootJar --no-daemon

# ---- Run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
# MaxRAMPercentage ayuda a entrar en los 512 MB del free tier de Render
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75.0 -jar app.jar"]
