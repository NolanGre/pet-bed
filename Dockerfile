FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
RUN chmod +x gradlew
COPY build.gradle .
COPY settings.gradle .
RUN ./gradlew dependencies --no-daemon
COPY src src
RUN ./gradlew bootJar --no-daemon
RUN java -Djarmode=layertools -jar build/libs/*.jar extract --destination build/extracted

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S petbed && adduser -S petbed -G petbed
WORKDIR /app
COPY --from=builder --chown=petbed:petbed /app/build/extracted/dependencies ./
COPY --from=builder --chown=petbed:petbed /app/build/extracted/spring-boot-loader ./
COPY --from=builder --chown=petbed:petbed /app/build/extracted/snapshot-dependencies ./
COPY --from=builder --chown=petbed:petbed /app/build/extracted/application ./
USER petbed
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
