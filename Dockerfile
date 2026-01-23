FROM openjdk:8-jdk

# Create group and user before copying files
RUN groupadd --system --gid 800 customgroup \
    && useradd --system --uid 800 --gid customgroup --shell /bin/sh customuser

# Use a build argument for version
ARG APP_VERSION

# Copy the JAR file
COPY build/libs/laa-ccms-service-adapter-${APP_VERSION}.jar assess-service-adapter.jar

# Change ownership of the JAR
RUN chown customuser:customgroup assess-service-adapter.jar

# Switch to non-root user
USER 800

EXPOSE 8080
ENV TZ=Europe/London
ENTRYPOINT ["java", "-jar", "assess-service-adapter.jar"]
