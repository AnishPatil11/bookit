# ---- build stage: compile and package with Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- run stage: small JRE image, non-root user ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 bookit
COPY --from=build /app/target/bookit.jar app.jar
USER bookit
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
