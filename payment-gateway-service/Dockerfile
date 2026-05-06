FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/payment-gateway-service-1.0.0.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
