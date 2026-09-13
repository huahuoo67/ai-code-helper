FROM eclipse-temurin:21-jre

WORKDIR /app

COPY app/ai-code-helper.jar /app/ai-code-helper.jar

EXPOSE 8081

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/ai-code-helper.jar", "--server.address=0.0.0.0", "--server.port=8081"]
