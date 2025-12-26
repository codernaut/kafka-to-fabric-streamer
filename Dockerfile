# syntax=docker/dockerfile:1.6

FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /work
COPY --from=build /workspace/target/quarkus-app/lib/ ./lib/
COPY --from=build /workspace/target/quarkus-app/app/ ./app/
COPY --from=build /workspace/target/quarkus-app/quarkus/ ./quarkus/
COPY --from=build /workspace/target/quarkus-app/*.jar ./
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["java","-jar","./quarkus-run.jar"]
