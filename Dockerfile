# ============================================================
# STAGE 1: BUILD
# Dung Maven + JDK 21 de compile va dong goi thanh file .jar
# ============================================================
FROM maven:3.9.9-amazoncorretto-21 AS builder

WORKDIR /app

# Copy pom.xml truoc de tan dung Docker layer cache
# Neu chi thay doi code (khong thay doi dependency)
# → Docker skip buoc download dependency → build nhanh hon nhieu
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# Copy toan bo source code
COPY src ./src

# Build file .jar, bo qua tests
RUN mvn package -Dmaven.test.skip=true --batch-mode --no-transfer-progress


# ============================================================
# STAGE 2: RUNTIME
# Chi copy file .jar da build
# Image production nho gon: khong co Maven, khong co JDK
# ============================================================
FROM amazoncorretto:21-alpine

LABEL maintainer="BE-AI-Study-Hub Team"
LABEL description="AI Study Hub Backend Service"

WORKDIR /app

# Tao user non-root de chay app (bao mat tot hon)
RUN addgroup -S spring && adduser -S spring -G spring

# Copy file .jar tu stage build sang stage runtime
# Ten file: BE-0.0.1-SNAPSHOT.jar (theo artifactId=BE, version=0.0.1-SNAPSHOT trong pom.xml)
COPY --from=builder /app/target/BE-0.0.1-SNAPSHOT.jar app.jar

RUN chown spring:spring app.jar

USER spring

# Spring Boot chay tren port 8080 mac dinh
EXPOSE 8080

# JVM flags toi uu cho container:
# -XX:+UseContainerSupport  → JVM tu nhan dien RAM gioi han cua container (khong lay RAM cua host)
# -XX:MaxRAMPercentage=75.0 → Dung toi da 75% RAM duoc cap cho container (tranh OOM kill)
# -Djava.security.egd=...   → Tang toc startup cua Spring Boot
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]