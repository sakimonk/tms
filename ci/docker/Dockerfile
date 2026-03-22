# syntax=docker/dockerfile:1
# 构建上下文：仓库根目录
# docker build -f ci/docker/Dockerfile -t tms:latest .

# ---------- 构建阶段 ----------
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package \
    && rm -rf /root/.m2/repository

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system --gid 1000 spring \
    && useradd --system --uid 1000 --gid spring --shell /usr/sbin/nologin spring

WORKDIR /app

COPY --from=builder /build/target/tms-*.jar /app/app.jar
RUN chown spring:spring /app/app.jar

USER spring

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# SpringDoc OpenAPI 文档可用时返回 200；未引入时可改为 TCP 探测或去掉
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS -o /dev/null http://127.0.0.1:8080/v3/api-docs || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
