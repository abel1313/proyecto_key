
# Etapa 1: compilar con Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos el código fuente
COPY pom.xml .
COPY src ./src

# Compilamos y empaquetamos
RUN mvn clean package -DskipTests

# Etapa 2: imagen final ligera
FROM eclipse-temurin:17-jdk-alpine
COPY --from=build /app/target/mis-productos-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

