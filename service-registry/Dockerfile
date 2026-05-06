FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/service-registry-1.0.0.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-Xms128m", "-Xmx256m", "-jar", "app.jar"]
