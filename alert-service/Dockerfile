FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/alert-service-1.0.0.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-Xms128m", "-Xmx384m", "-jar", "app.jar"]
