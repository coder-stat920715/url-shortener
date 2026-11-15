# Build any module:  docker build --build-arg MODULE=redirection-service -t shortener/redirection .
FROM maven:3.9-eclipse-temurin-17 AS build
ARG MODULE
WORKDIR /src
COPY . .
RUN mvn -q -B -pl ${MODULE} -am -DskipTests package

FROM eclipse-temurin:17-jre
ARG MODULE
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75"
COPY --from=build /src/${MODULE}/target/${MODULE}.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
