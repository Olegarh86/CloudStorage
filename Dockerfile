FROM eclipse-temurin:17-jre
WORKDIR /app
COPY build/libs/*.jar CloudStorage.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "CloudStorage.jar"]