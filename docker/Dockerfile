FROM openjdk:21-jdk-slim
WORKDIR /app
COPY docker/daisimao-server-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["/bin/sh", "-c", "java -jar -Xms256m -Xmx512m app.jar"]
