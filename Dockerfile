# Etapa 1: construir el proyecto con Maven
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: ejecutar el .jar generado
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /app/target/mis-productos-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
CMD ["java", "-jar", "app.jar"]
