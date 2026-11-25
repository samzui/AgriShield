FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Maven Central mirror + retry handling are configured in .mvn/settings.xml.
COPY .mvn/settings.xml /root/.m2/settings.xml
RUN for i in 1 2 3 4 5; do mvn -B -Dmaven.test.skip=true dependency:go-offline && exit 0; echo "Maven dependency download attempt $i failed; retrying..."; sleep 5; done; exit 1
# Production container: compile/package application sources, but do not compile or run test sources.
RUN mvn -B -Dmaven.test.skip=true package
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
