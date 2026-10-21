FROM maven:3.9.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /build
COPY pom.xml .
COPY config-server/pom.xml config-server/
COPY discovery-server/pom.xml discovery-server/
COPY event-service/pom.xml event-service/
COPY gateway/pom.xml gateway/
RUN mvn -q -B -pl ${MODULE} -am dependency:go-offline
COPY . .
RUN mvn -q -B -pl ${MODULE} -am -DskipTests package

FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
COPY --from=build /build/${MODULE}/target/${MODULE}-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
