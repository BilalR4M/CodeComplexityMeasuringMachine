FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.source="https://github.com/BilalR4M/CodeComplexityMeasuringMachine"
RUN useradd --system --uid 10001 ccmm
WORKDIR /app
COPY target/ccmm-collab-server-*-all.jar /app/server.jar
USER ccmm
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/server.jar"]
