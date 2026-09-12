FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/co-lending-control-tower-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

