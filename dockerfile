# ===== build stage =====
FROM maven:3.9.8-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

# 1) Semillas de caché (solo POMs)
COPY pom.xml ./
COPY lib/pom.xml lib/pom.xml
COPY core/pom.xml core/pom.xml
COPY api-service/pom.xml api-service/pom.xml

# 2) Resolver dependencias (cachea ~/.m2)
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -DskipTests dependency:go-offline

# 3) Copiar fuentes y empaquetar api-service
COPY lib lib
COPY core core
COPY api-service api-service

RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -DskipTests -pl api-service -am package

# ===== runtime stage =====
FROM eclipse-temurin:21-jre-alpine
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0" \
    SPRING_PROFILES_ACTIVE=prod \
    TZ=UTC
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app
COPY --from=build /workspace/api-service/target/*.jar /app/app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["/bin/sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
