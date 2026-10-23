FROM maven:3.9.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /build
COPY pom.xml .
COPY config-server/pom.xml config-server/
COPY discovery-server/pom.xml discovery-server/
COPY event-service/pom.xml event-service/
COPY user-service/pom.xml user-service/
COPY booking-service/pom.xml booking-service/
COPY gateway/pom.xml gateway/
RUN mvn -q -B -pl ${MODULE} -am dependency:go-offline
COPY . .
RUN mvn -q -B -pl ${MODULE} -am -DskipTests package
# configs-repo в рабочей копии — подмодуль, его .git указывает наружу и в образ не попадает.
# Делаем из снимка каталога самостоятельный git-репозиторий, чтобы config-server мог его читать.
RUN test -f configs-repo/application.yml || { echo "configs-repo is empty, run: git submodule update --init"; exit 1; } \
    && rm -rf configs-repo/.git \
    && git -C configs-repo init -q -b main \
    && git -C configs-repo add -A \
    && git -C configs-repo -c user.name=build -c user.email=build@local commit -qm "snapshot for image"

FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
COPY --from=build /build/${MODULE}/target/${MODULE}-*.jar app.jar
COPY --from=build /build/configs-repo /configs-repo
ENTRYPOINT ["java", "-jar", "app.jar"]
