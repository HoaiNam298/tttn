FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S shopapp && adduser -S shopapp -G shopapp
WORKDIR /app
RUN mkdir /app/uploads && chown shopapp:shopapp /app/uploads
COPY --from=build /workspace/target/shopapp-*.jar app.jar
USER shopapp
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
